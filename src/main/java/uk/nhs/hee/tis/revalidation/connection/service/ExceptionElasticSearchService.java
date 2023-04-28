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

import java.util.ArrayList;
import org.elasticsearch.common.util.iterable.Iterables;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionSummaryDto;
import uk.nhs.hee.tis.revalidation.connection.entity.DiscrepanciesView;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionView;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.DiscrepanciesElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.connection.repository.ExceptionElasticSearchRepository;

@Service
public class ExceptionElasticSearchService {

  private static final Logger LOG = LoggerFactory.getLogger(ExceptionElasticSearchService.class);

  @Autowired
  ExceptionElasticSearchRepository exceptionElasticSearchRepository;

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
      LOG.error("An exception occurred while attempting to do an ES search - Exception index", re);
      throw re;
    }
  }

  /**
   * add new exceptions to elasticsearch index.
   *
   * @param dataToSave exceptions to go in elasticsearch
   */
  public void saveExceptionViews(ExceptionView dataToSave) {
    // find trainee record from Exception ES index
    Iterable<ExceptionView> existingRecords = findExceptionViewsByGmcNumberPersonId(dataToSave);

    // if trainee already exists in ES index, then update the existing record
    if (Iterables.size(existingRecords) > 0) {
      updateExceptionViews(existingRecords, dataToSave);
    }
    // otherwise, add a new record
    else {
      addExceptionViews(dataToSave);
    }
  }

  /**
   * remove exceptions from elasticsearch index by gmcReferenceNumber.
   *
   * @param gmcReferenceNumber id of exception to remove
   */
  public void removeExceptionViewByGmcNumber(String gmcReferenceNumber) {
    if (gmcReferenceNumber != null) {
      try {
        exceptionElasticSearchRepository.deleteByGmcReferenceNumber(gmcReferenceNumber);
      }
      catch (Exception ex) {
        LOG.info("Exception in `removeExceptionViewByGmcNumber` (GmcId: {}): {}",
            gmcReferenceNumber,  ex);
      }
    }
  }

  /**
   * remove exceptions from elasticsearch index by tcsPersonId.
   *
   * @param tcsPersonId id of exception to remove
   */
  public void removeExceptionViewByTcsPersonId(Long tcsPersonId) {
    if (tcsPersonId != null) {
      try {
        exceptionElasticSearchRepository.deleteByTcsPersonId(tcsPersonId);
      }
      catch (Exception ex) {
        LOG.info("Exception in `removeExceptionViewByTcsPersonId` (PersonId: {}): {}",
            tcsPersonId,  ex);
      }
    }
  }

  /**
   * find existing exceptions from elasticsearch index by gmcNumber or tcsPersonId.
   *
   * @param dataToSave exception to be searched in elasticsearch
   */
  private Iterable<ExceptionView> findExceptionViewsByGmcNumberPersonId(ExceptionView dataToSave) {
    Iterable<ExceptionView> result = new ArrayList<>();
    var mustBetweenDifferentColumnFilters = new BoolQueryBuilder();
    var shouldQuery = new BoolQueryBuilder();

    if (dataToSave.getGmcReferenceNumber() != null) {
      shouldQuery
          .should(new MatchQueryBuilder("gmcReferenceNumber", dataToSave.getGmcReferenceNumber()));
    }
    if (dataToSave.getTcsPersonId() != null) {
      shouldQuery
          .should(new MatchQueryBuilder("tcsPersonId", dataToSave.getTcsPersonId()));
    }

    BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);
    try {
      result = exceptionElasticSearchRepository.search(fullQuery);
    }
    catch (Exception ex) {
      LOG.info("Exception in `findExceptionViewsByGmcNumberPersonId` (GmcId: {}; PersonId: {}): {}",
          dataToSave.getGmcReferenceNumber(),dataToSave.getTcsPersonId(),  ex);
    }

    return result;
  }

  /**
   * add new exceptions to elasticsearch index.
   *
   * @param dataToSave exceptions to go in elasticsearch
   */
  private void addExceptionViews(ExceptionView dataToSave) {
    try {
      exceptionElasticSearchRepository.save(dataToSave);
    }
    catch (Exception ex) {
      LOG.info("Exception in `addConnectedViews` (GmcId: {}; PersonId: {}): {}",
          dataToSave.getGmcReferenceNumber(),dataToSave.getTcsPersonId(),  ex);
    }
  }

  /**
   * update existing exceptions to elasticsearch index.
   *
   * @param existingRecords existing exceptions to be updated in elasticsearch
   * @param dataToSave      new exceptions details to be saved in elasticsearch
   */
  private void updateExceptionViews(Iterable<ExceptionView> existingRecords,
      ExceptionView dataToSave) {
    existingRecords.forEach(exceptionView -> {
      dataToSave.setId(exceptionView.getId());
      try {
        exceptionElasticSearchRepository.save(dataToSave);
      }
      catch (Exception ex) {
        LOG.info("Exception in `updateExceptionViews` (GmcId: {}; PersonId: {}): {}",
            dataToSave.getGmcReferenceNumber(),dataToSave.getTcsPersonId(),  ex);
      }
    });
  }
}
