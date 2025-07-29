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

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionSummaryDto;
import uk.nhs.hee.tis.revalidation.connection.entity.DiscrepanciesView;
import uk.nhs.hee.tis.revalidation.connection.exception.ConnectionQueryException;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.DiscrepanciesElasticSearchRepository;

@Service
public class DiscrepanciesElasticSearchService {

  @Autowired
  DiscrepanciesElasticSearchRepository discrepanciesElasticSearchRepository;

  @Autowired
  ConnectionInfoMapper connectionInfoMapper;

  /**
   * Get discrepancies from discrepancies elasticsearch index.
   *
   * @param searchQuery   query to run
   * @param dbcs          list of dbcs to limit the search to
   * @param programmeName programme name to filter by
   * @param pageable      pagination information
   */
  public ConnectionSummaryDto searchForPage(String searchQuery, List<String> dbcs,
      String programmeName, Pageable pageable)
      throws ConnectionQueryException {
    try {
      Page<DiscrepanciesView> result = discrepanciesElasticSearchRepository
          .findAll(searchQuery,
              ElasticsearchQueryHelper.formatDesignatedBodyCodesForElasticsearchQuery(dbcs),
              programmeName,
              pageable);

      final var discrepancies = result.get().collect(toList());
      return ConnectionSummaryDto.builder()
          .totalPages(result.getTotalPages())
          .totalResults(result.getTotalElements())
          .connections(connectionInfoMapper.discrepancyToConnectionInfoDtos(discrepancies))
          .build();

    } catch (RuntimeException re) {
      throw new ConnectionQueryException("discrepancies (all)", searchQuery, re);
    }
  }

  /**
   * Get discrepancies that should be connected from discrepancies elasticsearch index.
   *
   * @param searchQuery   query to run
   * @param dbcs          list of dbcs to limit the search to
   * @param programmeName programme name to filter by
   * @param pageable      pagination information
   */
  public ConnectionSummaryDto searchForPageToConnect(String searchQuery, List<String> dbcs,
      String programmeName, Pageable pageable)
      throws ConnectionQueryException {
    try {
      Page<DiscrepanciesView> result = discrepanciesElasticSearchRepository
          .findAllToConnect(searchQuery,
              ElasticsearchQueryHelper.formatDesignatedBodyCodesForElasticsearchQuery(dbcs),
              programmeName,
              pageable);

      final var discrepancies = result.get().collect(toList());
      return ConnectionSummaryDto.builder()
          .totalPages(result.getTotalPages())
          .totalResults(result.getTotalElements())
          .connections(connectionInfoMapper.discrepancyToConnectionInfoDtos(discrepancies))
          .build();

    } catch (RuntimeException re) {
      throw new ConnectionQueryException("discrepancies (connectable)", searchQuery, re);
    }
  }

  /**
   * Get discrepancies that should be disconnected from discrepancies elasticsearch index.
   *
   * @param searchQuery query to run
   * @param dbcs        list of dbcs to limit the search to
   * @param pageable    pagination information
   */
  public ConnectionSummaryDto searchForPageToDisconnect(String searchQuery, List<String> dbcs,
      Pageable pageable)
      throws ConnectionQueryException {
    try {
      Page<DiscrepanciesView> result = discrepanciesElasticSearchRepository
          .findAllToDisconnect(searchQuery,
              ElasticsearchQueryHelper.formatDesignatedBodyCodesForElasticsearchQuery(dbcs),
              pageable);

      final var discrepancies = result.get().collect(toList());
      return ConnectionSummaryDto.builder()
          .totalPages(result.getTotalPages())
          .totalResults(result.getTotalElements())
          .connections(connectionInfoMapper.discrepancyToConnectionInfoDtos(discrepancies))
          .build();

    } catch (RuntimeException re) {
      throw new ConnectionQueryException("discrepancies (disconnectable)", searchQuery, re);
    }
  }
}
