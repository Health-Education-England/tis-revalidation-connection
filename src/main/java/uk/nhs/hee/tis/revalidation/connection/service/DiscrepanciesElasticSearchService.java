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
import uk.nhs.hee.tis.revalidation.connection.entity.DiscrepanciesView;
import uk.nhs.hee.tis.revalidation.connection.exception.ConnectionQueryException;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.DiscrepanciesElasticSearchRepository;

@Service
public class DiscrepanciesElasticSearchService {

  private final static String DOCTOR_FIRST_NAME_DISCREPANCIES = "doctorFirstName";
  private final static String DOCTOR_LAST_NAME_DISCREPANCIES = "doctorLastName";
  private final static String GMC_REFERENCE_NUMBER_DISCREPANCIES = "gmcReferenceNumber";
  private final static String PROGRAMME_NAME_DISCREPANCIES = "programmeName";
  private final static String DESIGNATED_BODY_DISCREPANCIES = "designatedBody";
  private final static String TCS_DESIGNATED_BODY_DISCREPANCIES = "tcsDesignatedBody";
  private final static String MEMBERSHIP_TYPE_DISCREPANCIES = "membershipType";
  private final static String PLACEMENT_GRADE_DISCREPANCIES = "placementGrade";
  private final static String EXCLUDED_PLACEMENT_GRADE_DISCREPANCIES = "279";
  private final static String EXCLUDED_MEMBERSHIP_TYPE = "MILITARY";
  private final static String PROGRAMME_MEMBERSHIP_END_DATE_FIELD = "membershipEndDate";
  @Autowired
  DiscrepanciesElasticSearchRepository discrepanciesElasticSearchRepository;
  @Autowired
  ConnectionInfoMapper connectionInfoMapper;
  @Autowired
  private ElasticsearchOperations elasticsearchOperations;

  /**
   * Get discrepancies from discrepancies elasticsearch index.
   *
   * @param searchQuery   query to run
   * @param dbcs          list of gmc dbcs to limit the search to
   * @param tisDbcs       list of tis dbcs to limit the search to
   * @param programmeName programme name to filter by
   * @param pageable      pagination information
   */
  public ConnectionSummaryDto searchForPage(String searchQuery, List<String> dbcs,
      List<String> tisDbcs,
      String programmeName, Pageable pageable)
      throws ConnectionQueryException {
    try {
      Page<DiscrepanciesView> result = discrepanciesElasticSearchRepository
          .findAll(searchQuery,
              ElasticsearchQueryHelper.formatDesignatedBodyCodesForElasticsearchQuery(dbcs),
              ElasticsearchQueryHelper.formatDesignatedBodyCodesForElasticsearchQuery(tisDbcs),
              programmeName,
              pageable);

      final var discrepancies = result.get().collect(toList());
      return ConnectionSummaryDto.builder()
          .totalPages(result.getTotalPages())
          .totalResults(result.getTotalElements())
          .connections(connectionInfoMapper.discrepancyToConnectionInfoDtos(discrepancies))
          .build();

    } catch (RuntimeException re) {
      throw new ConnectionQueryException("discrepancies", searchQuery, re);
    }
  }

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
  public ConnectionSummaryDto searchForPageWithMembershipEndDate(String searchQuery,
      List<String> dbcs,
      List<String> tisDbcs,
      String programmeName,
      LocalDate membershipEndDateFrom,
      LocalDate membershipEndDateTo,
      Pageable pageable)
      throws ConnectionQueryException {

    try {
      BoolQueryBuilder rootQuery = QueryBuilders.boolQuery();

      rootQuery.filter(
          QueryBuilders.boolQuery()
              .mustNot(
                  QueryBuilders.matchQuery(MEMBERSHIP_TYPE_DISCREPANCIES, EXCLUDED_MEMBERSHIP_TYPE))
      );

      rootQuery.filter(
          QueryBuilders.boolQuery()
              .mustNot(QueryBuilders.matchQuery(PLACEMENT_GRADE_DISCREPANCIES,
                  EXCLUDED_PLACEMENT_GRADE_DISCREPANCIES))
      );

      String formattedDbcs =
          ElasticsearchQueryHelper.formatDesignatedBodyCodesForElasticsearchQuery(dbcs);
      String formattedTisDbcs =
          ElasticsearchQueryHelper.formatDesignatedBodyCodesForElasticsearchQuery(tisDbcs);
      BoolQueryBuilder designatedBodyQuery = QueryBuilders.boolQuery()
          .should(QueryBuilders.matchQuery(DESIGNATED_BODY_DISCREPANCIES, formattedDbcs))
          .should(QueryBuilders.matchQuery(TCS_DESIGNATED_BODY_DISCREPANCIES, formattedTisDbcs))
          .minimumShouldMatch(1);
      rootQuery.filter(designatedBodyQuery);

      if (StringUtils.hasText(programmeName)) {
        rootQuery.filter(
            QueryBuilders.matchPhraseQuery(PROGRAMME_NAME_DISCREPANCIES, programmeName));
      }

      if (StringUtils.hasText(searchQuery)) {
        BoolQueryBuilder searchSubQuery = QueryBuilders.boolQuery()
            .should(QueryBuilders.wildcardQuery(DOCTOR_FIRST_NAME_DISCREPANCIES, searchQuery + "*"))
            .should(QueryBuilders.wildcardQuery(DOCTOR_LAST_NAME_DISCREPANCIES, searchQuery + "*"))
            .should(
                QueryBuilders.wildcardQuery(GMC_REFERENCE_NUMBER_DISCREPANCIES, searchQuery + "*"))
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
