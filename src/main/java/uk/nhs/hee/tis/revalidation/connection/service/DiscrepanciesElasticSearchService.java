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
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.elasticsearch.index.query.QueryBuilders.wildcardQuery;

import java.time.LocalDate;
import java.util.List;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
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
import uk.nhs.hee.tis.revalidation.connection.entity.DiscrepanciesView;
import uk.nhs.hee.tis.revalidation.connection.exception.ConnectionQueryException;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;

@Service
public class DiscrepanciesElasticSearchService {

  private static final String DOCTOR_FIRST_NAME_DISCREPANCIES = "doctorFirstName";
  private static final String DOCTOR_LAST_NAME_DISCREPANCIES = "doctorLastName";
  private static final String GMC_REFERENCE_NUMBER_DISCREPANCIES = "gmcReferenceNumber";
  private static final String PROGRAMME_NAME_DISCREPANCIES = "programmeName";
  private static final String DESIGNATED_BODY_DISCREPANCIES = "designatedBody";
  private static final String TCS_DESIGNATED_BODY_DISCREPANCIES = "tcsDesignatedBody";
  private static final String MEMBERSHIP_TYPE_DISCREPANCIES = "membershipType";
  private static final String PLACEMENT_GRADE_DISCREPANCIES = "placementGrade";
  private static final String EXCLUDED_PLACEMENT_GRADE_DISCREPANCIES = "279";
  private static final String EXCLUDED_MEMBERSHIP_TYPE = "MILITARY";
  private static final String PROGRAMME_MEMBERSHIP_END_DATE_FIELD = "membershipEndDate";

  @Autowired
  ConnectionInfoMapper connectionInfoMapper;
  @Autowired
  private ElasticsearchOperations elasticsearchOperations;

  /**
   * Get trainees with discrepancies from discrepancies elasticsearch index when pm end date values
   * are there for filtering.
   *
   * @param searchQuery           query to run
   * @param pageable              pagination information
   * @param dbcs                  designated body
   * @param tisDbcs               tis designated body
   * @param programmeName         programme name
   * @param membershipEndDateFrom range of date from
   * @param membershipEndDateTo   range of date to
   */
  public ConnectionSummaryDto searchForDiscrepanciesPageWithFilters(String searchQuery,
      List<String> dbcs,
      List<String> tisDbcs,
      String programmeName,
      LocalDate membershipEndDateFrom,
      LocalDate membershipEndDateTo,
      Pageable pageable)
      throws ConnectionQueryException {

    try {
      BoolQueryBuilder rootQuery = boolQuery();

      rootQuery.filter(
          boolQuery().mustNot(matchQuery(MEMBERSHIP_TYPE_DISCREPANCIES, EXCLUDED_MEMBERSHIP_TYPE)));

      rootQuery.filter(boolQuery().mustNot(
          matchQuery(PLACEMENT_GRADE_DISCREPANCIES, EXCLUDED_PLACEMENT_GRADE_DISCREPANCIES)));

      String formattedDbcs =
          ElasticsearchQueryHelper.formatDesignatedBodyCodesForElasticsearchQuery(dbcs);
      String formattedTisDbcs =
          ElasticsearchQueryHelper.formatDesignatedBodyCodesForElasticsearchQuery(tisDbcs);
      BoolQueryBuilder designatedBodyQuery = boolQuery()
          .should(matchQuery(DESIGNATED_BODY_DISCREPANCIES, formattedDbcs))
          .should(matchQuery(TCS_DESIGNATED_BODY_DISCREPANCIES, formattedTisDbcs))
          .minimumShouldMatch(1);
      rootQuery.filter(designatedBodyQuery);

      if (StringUtils.hasText(programmeName)) {
        rootQuery.filter(
            matchPhraseQuery(PROGRAMME_NAME_DISCREPANCIES, programmeName).zeroTermsQuery(
                ZeroTermsQuery.ALL));
      }

      if (StringUtils.hasText(searchQuery)) {
        BoolQueryBuilder searchSubQuery = boolQuery()
            .should(wildcardQuery(DOCTOR_FIRST_NAME_DISCREPANCIES, searchQuery + "*"))
            .should(wildcardQuery(DOCTOR_LAST_NAME_DISCREPANCIES, searchQuery + "*"))
            .should(
                wildcardQuery(GMC_REFERENCE_NUMBER_DISCREPANCIES, searchQuery + "*"))
            .minimumShouldMatch(1);

        rootQuery.filter(searchSubQuery);
      }

      if (membershipEndDateFrom != null || membershipEndDateTo != null) {
        RangeQueryBuilder dateRange = rangeQuery(PROGRAMME_MEMBERSHIP_END_DATE_FIELD);
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

      SearchHits<DiscrepanciesView> searchHits =
          elasticsearchOperations.search(searchQueryEsResult, DiscrepanciesView.class);

      List<DiscrepanciesView> contentList = searchHits.getSearchHits()
          .stream()
          .map(SearchHit::getContent)
          .collect(toList());

      Page<DiscrepanciesView> page =
          new PageImpl<>(contentList, pageable, searchHits.getTotalHits());

      final var discrepanciesTrainees = page.get().collect(toList());

      return ConnectionSummaryDto.builder()
          .totalPages(page.getTotalPages())
          .totalResults(page.getTotalElements())
          .connections(
              connectionInfoMapper.discrepancyToConnectionInfoDtos(discrepanciesTrainees))
          .build();

    } catch (RuntimeException re) {
      throw new ConnectionQueryException("discrepancies", searchQuery, re);
    }
  }
}
