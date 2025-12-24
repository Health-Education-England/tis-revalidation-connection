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
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchPhraseQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.wildcardQuery;

import java.time.LocalDate;
import java.util.List;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.search.MatchQuery.ZeroTermsQuery;
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
import uk.nhs.hee.tis.revalidation.connection.service.util.EsQueryUtils;

@Service
public class ConnectedElasticSearchService {

  private static final String DOCTOR_FIRST_NAME_FIELD = "doctorFirstName";
  private static final String DOCTOR_LAST_NAME_FIELD = "doctorLastName";
  private static final String GMC_REFERENCE_NUMBER_FIELD = "gmcReferenceNumber";
  private static final String PROGRAMME_NAME_FIELD = "programmeName";
  private static final String DESIGNATED_BODY_FIELD = "designatedBody";
  private static final String MEMBERSHIP_TYPE_FIELD = "membershipType";
  private static final String PLACEMENT_GRADE_FIELD = "placementGrade";
  private static final String EXCLUDED_PLACEMENT_GRADE = "279";
  private static final String EXCLUDED_MEMBERSHIP_TYPE = "MILITARY";
  private static final String PROGRAMME_MEMBERSHIP_END_DATE_FIELD = "membershipEndDate";
  private static final String GMC_SUBMISSION_DATE_FIELD = "submissionDate";

  @Autowired
  ConnectionInfoMapper connectionInfoMapper;
  @Autowired
  private ElasticsearchOperations elasticsearchOperations;

  /**
   * Get connected trainees from Connected elasticsearch index when pm end date values are there for
   * filtering.
   *
   * @param searchQuery           query to run
   * @param pageable              pagination information
   * @param dbcs                  designated body
   * @param programmeName         programme name
   * @param membershipEndDateFrom range of date from
   * @param membershipEndDateTo   range of date to
   */
  public ConnectionSummaryDto searchForConnectionPageWithFilters(String searchQuery,
      List<String> dbcs,
      String programmeName,
      LocalDate membershipEndDateFrom,
      LocalDate membershipEndDateTo,
      LocalDate gmcSubmissionDateFrom,
      LocalDate gmcSubmissionDateTo,
      Pageable pageable)
      throws ConnectionQueryException {

    try {
      BoolQueryBuilder rootQuery = boolQuery();

      rootQuery.filter(
          boolQuery().mustNot(matchQuery(MEMBERSHIP_TYPE_FIELD, EXCLUDED_MEMBERSHIP_TYPE)));

      rootQuery.filter(
          boolQuery().mustNot(matchQuery(PLACEMENT_GRADE_FIELD, EXCLUDED_PLACEMENT_GRADE)));

      String formattedDbcs = ElasticsearchQueryHelper
          .formatDesignatedBodyCodesForElasticsearchQuery(dbcs);
      if (StringUtils.hasText(formattedDbcs)) {
        rootQuery.filter(matchQuery(DESIGNATED_BODY_FIELD, formattedDbcs));
      }

      if (StringUtils.hasText(programmeName)) {
        rootQuery.filter(matchPhraseQuery(PROGRAMME_NAME_FIELD, programmeName).zeroTermsQuery(
            ZeroTermsQuery.ALL));
      }

      if (StringUtils.hasText(searchQuery)) {
        BoolQueryBuilder searchSubQuery = boolQuery()
            .should(wildcardQuery(DOCTOR_FIRST_NAME_FIELD, searchQuery + "*"))
            .should(wildcardQuery(DOCTOR_LAST_NAME_FIELD, searchQuery + "*"))
            .should(wildcardQuery(GMC_REFERENCE_NUMBER_FIELD, searchQuery + "*"))
            .minimumShouldMatch(1);

        rootQuery.filter(searchSubQuery);
      }

      EsQueryUtils.addDateRangeFilter(rootQuery,
          PROGRAMME_MEMBERSHIP_END_DATE_FIELD,
          membershipEndDateFrom != null ? membershipEndDateFrom.toString() : null,
          membershipEndDateTo != null ? membershipEndDateTo.toString() : null);

      EsQueryUtils.addDateRangeFilter(rootQuery,
          GMC_SUBMISSION_DATE_FIELD,
          gmcSubmissionDateFrom != null ? gmcSubmissionDateFrom.toString() : null,
          gmcSubmissionDateTo != null ? gmcSubmissionDateTo.toString() : null);

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
