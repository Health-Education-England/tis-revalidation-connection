/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.revalidation.connection.service;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.List;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionSummaryDto;
import uk.nhs.hee.tis.revalidation.connection.entity.CurrentConnectionsView;
import uk.nhs.hee.tis.revalidation.connection.exception.ConnectionQueryException;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.CurrentConnectionElasticSearchRepository;

@Service
public class ConnectedElasticSearchService {

  @Autowired
  CurrentConnectionElasticSearchRepository currentConnectionElasticSearchRepository;

  @Autowired
  ConnectionInfoMapper connectionInfoMapper;

  @Autowired
  private ElasticsearchOperations elasticsearchOperations;

  private final static String DOCTOR_FIRST_NAME_FIELD = "doctorFirstName";
  private final static String DOCTOR_LAST_NAME_FIELD = "doctorLastName";
  private final static String GMC_REFERENCE_NUMBER_FIELD = "gmcReferenceNumber";
  private final static String PROGRAMME_NAME_FIELD = "programmeName";
  private final static String DESIGNATED_BODY_FIELD = "designatedBody";
  private final static String MEMBERSHIP_TYPE_FIELD = "membershipType";
  private final static String PLACEMENT_GRADE_FIELD = "placementGrade";
  private final static String EXCLUDED_PLACEMENT_GRADE = "279";
  private final static String EXCLUDED_MEMBERSHIP_TYPE = "MILITARY";
  private final static String PROGRAMME_MEMBERSHIP_END_DATE_FIELD = "membershipEndDate";

  /**
   * Get connected trainees from Connected elasticsearch index.
   *
   * @param searchQuery query to run
   * @param pageable    pagination information
   */
  public ConnectionSummaryDto searchForPage(String searchQuery, List<String> dbcs,
      String programmeName, Pageable pageable) throws ConnectionQueryException {

    try {
      Page<CurrentConnectionsView> result = currentConnectionElasticSearchRepository
          .findAll(searchQuery,
              ElasticsearchQueryHelper.formatDesignatedBodyCodesForElasticsearchQuery(dbcs),
              programmeName,
              pageable);

      final var connectedTrainees = result.get().collect(toList());
      return ConnectionSummaryDto.builder()
          .totalPages(result.getTotalPages())
          .totalResults(result.getTotalElements())
          .connections(connectionInfoMapper
              .currentConnectionsToConnectionInfoDtos(connectedTrainees))
          .build();

    } catch (RuntimeException re) {
      throw new ConnectionQueryException("current connections", searchQuery, re);
    }
  }

  /**
   * Get connected trainees from Connected elasticsearch index
   * when pm end date values are there for filtering.
   *
   * @param searchQuery query to run
   * @param pageable    pagination information
   * @param dbcs designated body
   * @param programmeName programme name
   * @param membershipEndDateFrom range of date from
   * @param membershipEndDateTo range of date to
   */
  public ConnectionSummaryDto searchForPageWithMembershipEndDate(String searchQuery,
      List<String> dbcs,
      String programmeName,
      LocalDate membershipEndDateFrom,
      LocalDate membershipEndDateTo,
      Pageable pageable)
      throws ConnectionQueryException {

    try {
      BoolQueryBuilder rootQuery = QueryBuilders.boolQuery();

      rootQuery.filter(
          QueryBuilders.boolQuery()
              .mustNot(QueryBuilders.matchQuery(MEMBERSHIP_TYPE_FIELD, EXCLUDED_MEMBERSHIP_TYPE))
      );

      rootQuery.filter(
          QueryBuilders.boolQuery()
              .mustNot(QueryBuilders.matchQuery(PLACEMENT_GRADE_FIELD, EXCLUDED_PLACEMENT_GRADE))
      );

      String formattedDbcs =
          ElasticsearchQueryHelper.formatDesignatedBodyCodesForElasticsearchQuery(dbcs);
      if (StringUtils.hasText(formattedDbcs)) {
        rootQuery.filter(QueryBuilders.matchQuery(DESIGNATED_BODY_FIELD, formattedDbcs));
      }

      if (StringUtils.hasText(programmeName)) {
        rootQuery.filter(QueryBuilders.matchPhraseQuery(PROGRAMME_NAME_FIELD, programmeName));
      }

      if (StringUtils.hasText(searchQuery)) {
        BoolQueryBuilder searchSubQuery = QueryBuilders.boolQuery()
            .should(QueryBuilders.wildcardQuery(DOCTOR_FIRST_NAME_FIELD, searchQuery + "*"))
            .should(QueryBuilders.wildcardQuery(DOCTOR_LAST_NAME_FIELD, searchQuery + "*"))
            .should(QueryBuilders.wildcardQuery(GMC_REFERENCE_NUMBER_FIELD, searchQuery + "*"))
            .minimumShouldMatch(1);

        rootQuery.filter(searchSubQuery);
      }

      if (membershipEndDateFrom != null || membershipEndDateTo != null) {
        RangeQueryBuilder dateRange = QueryBuilders.rangeQuery(PROGRAMME_MEMBERSHIP_END_DATE_FIELD);
        if (membershipEndDateFrom != null) {
          dateRange.gte(membershipEndDateFrom.toString());
        }
        if (membershipEndDateTo != null) {
          dateRange.lte(membershipEndDateTo.toString());
        }
        rootQuery.filter(dateRange);
      }

      NativeSearchQuery searchQueryEsResult = new NativeSearchQueryBuilder()
          .withQuery(rootQuery)
          .withPageable(pageable)
          .build();

      SearchHits<CurrentConnectionsView> searchHits =
          elasticsearchOperations.search(searchQueryEsResult, CurrentConnectionsView.class);

      List<CurrentConnectionsView> contentList = searchHits.getSearchHits()
          .stream()
          .map(SearchHit::getContent)
          .collect(toList());

      Page<CurrentConnectionsView> page =
          new PageImpl<>(contentList, pageable, searchHits.getTotalHits());

      final var connectedTrainees = page.get().collect(toList());

      return ConnectionSummaryDto.builder()
          .totalPages(page.getTotalPages())
          .totalResults(page.getTotalElements())
          .connections(
              connectionInfoMapper.currentConnectionsToConnectionInfoDtos(connectedTrainees))
          .build();

    } catch (RuntimeException re) {
      throw new ConnectionQueryException("current connections", searchQuery, re);
    }
  }
}
