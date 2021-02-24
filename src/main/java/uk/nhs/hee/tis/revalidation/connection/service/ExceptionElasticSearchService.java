package uk.nhs.hee.tis.revalidation.connection.service;


import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.elasticsearch.index.query.BoolQueryBuilder;
import uk.nhs.hee.tis.revalidation.connection.dto.ExceptionSummaryDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionView;
import uk.nhs.hee.tis.revalidation.connection.repository.ExceptionElasticSearchRepository;

import static java.util.stream.Collectors.toList;

@Service
public class ExceptionElasticSearchService {
  private static final Logger LOG = LoggerFactory.getLogger(ExceptionElasticSearchService.class);

  @Autowired
  ExceptionElasticSearchRepository exceptionElasticSearchRepository;

  public ExceptionSummaryDto searchForPage(String searchQuery, Pageable pageable) {

    try {
      // iterate over the column filters, if they have multiple values per filter, place a should between then
      // for each column filter set, place a must between them
      BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();

      //apply free text search on the searchable columns
      BoolQueryBuilder shouldQuery = applyTextBasedSearchQuery(searchQuery.toLowerCase());

      // add the free text query with a must to the column filters query
      BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);

      LOG.info("Query {}", fullQuery.toString());

      Page<ExceptionView> result = exceptionElasticSearchRepository.search(fullQuery, pageable);
//      LOG.info("Result {}", result.toString());

      final var exceptions = result.get().collect(toList());
      final var exceptionResponseDto = ExceptionSummaryDto.builder()
          .totalPages(result.getTotalPages())
          .totalResults(result.getTotalElements())
          .exceptionRecord(exceptions)
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
          .should(new MatchQueryBuilder("gmcReferenceNumber", searchQuery))
          .should(new WildcardQueryBuilder("doctorFirstName", "*" + searchQuery + "*"))
          .should(new WildcardQueryBuilder("doctorLastName", "*" + searchQuery + "*"));
    }

    //LOG.debug("Query is : {}", shouldQuery);
    return shouldQuery;
  }
}
