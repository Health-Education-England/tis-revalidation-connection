package uk.nhs.hee.tis.revalidation.connection.service;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.dto.ExceptionResponseDto;
import uk.nhs.hee.tis.revalidation.connection.entity.PersonView;
import uk.nhs.hee.tis.revalidation.connection.repository.PersonSearchRespository;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class PersonElasticSearchService {

  private static final Logger LOG = LoggerFactory.getLogger(PersonElasticSearchService.class);

  @Autowired
  PersonSearchRespository personElasticSearchRepository;

  public ExceptionResponseDto searchForPage(String searchQuery, Pageable pageable) {

    try {
      // iterate over the column filters, if they have multiple values per filter, place a should between then
      // for each column filter set, place a must between them
      BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();

      //apply free text search on the searchable columns
      BoolQueryBuilder shouldQuery = applyTextBasedSearchQuery(searchQuery);

      // add the free text query with a must to the column filters query
      BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);

      LOG.info("Query {}", fullQuery.toString());

      Page<PersonView> result = personElasticSearchRepository.search(fullQuery, pageable);
      LOG.info("Result {}", result.toString());

      final var exceptionLogs = result.get().collect(toList());
      final var exceptionResponseDto = ExceptionResponseDto.builder()
          .totalPages(result.getTotalPages())
          .totalResults(result.getTotalElements())
          .exceptionRecord(exceptionLogs)
          .build();
      return exceptionResponseDto;

    } catch (RuntimeException re) {
      LOG.error("An exception occurred while attempting to do an ES search", re);
      throw re;
    }
  }

  private BoolQueryBuilder applyTextBasedSearchQuery(String searchQuery) {
    // this part is the free text part of the query, place a should between all of the searchable fields
    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();
    if (StringUtils.isNotEmpty(searchQuery)) {
      searchQuery = StringUtils
          .remove(searchQuery, '"'); //remove any quotations that were added from the FE
      shouldQuery
          .should(new MatchQueryBuilder("publicHealthNumber", searchQuery))
          .should(new MatchQueryBuilder("fullName", searchQuery))
          .should(new WildcardQueryBuilder("surname", "*" + searchQuery + "*"))
          .should(new WildcardQueryBuilder("forenames", "*" + searchQuery + "*"))
          .should(new MatchQueryBuilder("gmcNumber", searchQuery))
          .should(new MatchQueryBuilder("gdcNumber", searchQuery))
          .should(new MatchQueryBuilder("role", searchQuery));

      if (StringUtils.isNumeric(searchQuery)) {
        shouldQuery = shouldQuery.should(new TermQueryBuilder("personId", searchQuery));
      }
    }

    //LOG.debug("Query is : {}", shouldQuery);
    return shouldQuery;
  }

}
