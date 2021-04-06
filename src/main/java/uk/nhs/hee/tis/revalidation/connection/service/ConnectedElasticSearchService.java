package uk.nhs.hee.tis.revalidation.connection.service;

import static java.util.stream.Collectors.toList;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionSummaryDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectedView;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.ConnectedElasticSearchRepository;

@Service
public class ConnectedElasticSearchService {
  private static final Logger LOG = LoggerFactory.getLogger(ConnectedElasticSearchService.class);

  @Autowired
  ConnectedElasticSearchRepository connectedElasticSearchRepository;

  @Autowired
  ConnectionInfoMapper connectionInfoMapper;


  /**
   * Get connected trainees from Connected elasticsearch index.
   *
   * @param searchQuery query to run
   * @param pageable pagination information
   */
  public ConnectionSummaryDto searchForPage(String searchQuery, Pageable pageable) {

    try {
      // for each column filter set, place a must between them
      BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();

      //apply free text search on the searchable columns
      BoolQueryBuilder shouldQuery = applyTextBasedSearchQuery(searchQuery.toLowerCase());

      // add the free text query with a must to the column filters query
      BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);

      LOG.debug("Query {}", fullQuery);

      Page<ConnectedView> result = connectedElasticSearchRepository.search(fullQuery, pageable);

      final var connectedTrainees = result.get().collect(toList());
      return ConnectionSummaryDto.builder()
          .totalPages(result.getTotalPages())
          .totalResults(result.getTotalElements())
          .connections(connectionInfoMapper.connectedToDtos(connectedTrainees))
          .build();

    } catch (RuntimeException re) {
      LOG.error("An exception occurred while attempting to do an ES search", re);
      throw re;
    }
  }

  private BoolQueryBuilder applyTextBasedSearchQuery(String searchQuery) {
    // place a should between all of the searchable fields
    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();
    if (StringUtils.isNotEmpty(searchQuery)) {
      searchQuery = StringUtils
          .remove(searchQuery, '"'); //remove any quotations that were added from the FE
      shouldQuery
          .should(new MatchQueryBuilder("gmcReferenceNumber", searchQuery))
          .should(new WildcardQueryBuilder("doctorFirstName", "*" + searchQuery + "*"))
          .should(new WildcardQueryBuilder("doctorLastName", "*" + searchQuery + "*"));
    }
    return shouldQuery;
  }
}
