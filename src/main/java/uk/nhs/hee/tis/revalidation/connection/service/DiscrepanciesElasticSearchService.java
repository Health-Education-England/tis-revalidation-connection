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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionSummaryDto;
import uk.nhs.hee.tis.revalidation.connection.entity.DiscrepanciesView;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.DiscrepanciesElasticSearchRepository;

@Service
public class DiscrepanciesElasticSearchService {

  private static final Logger LOG = LoggerFactory.getLogger(
      DiscrepanciesElasticSearchService.class);

  @Autowired
  DiscrepanciesElasticSearchRepository discrepanciesElasticSearchRepository;

  @Autowired
  ConnectionInfoMapper connectionInfoMapper;

  /**
   * Get exceptions from exception elasticsearch index.
   *
   * @param searchQuery query to run
   * @param pageable    pagination information
   */
  public ConnectionSummaryDto searchForPage(String searchQuery, Pageable pageable) {

    try {

      Page<DiscrepanciesView> result = discrepanciesElasticSearchRepository
          .findAll(searchQuery, pageable);

      final var exceptions = result.get().collect(toList());
      return ConnectionSummaryDto.builder()
          .totalPages(result.getTotalPages())
          .totalResults(result.getTotalElements())
          .connections(connectionInfoMapper.discrepancyToConnectionInfoDtos(exceptions))
          .build();

    } catch (RuntimeException re) {
      throw new RuntimeException(
          "An exception occurred while attempting to do an ES search - discrepancies", re);
    }
  }
}
