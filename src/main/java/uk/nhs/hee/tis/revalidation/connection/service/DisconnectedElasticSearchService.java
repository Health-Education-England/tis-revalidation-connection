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

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.util.iterable.Iterables;
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
import uk.nhs.hee.tis.revalidation.connection.entity.DisconnectedView;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.DisconnectedElasticSearchRepository;

@Service
public class DisconnectedElasticSearchService {

  private static final Logger LOG = LoggerFactory.getLogger(DisconnectedElasticSearchService.class);

  @Autowired
  DisconnectedElasticSearchRepository disconnectedElasticSearchRepository;

  @Autowired
  ConnectionInfoMapper connectionInfoMapper;


  /**
   * Get disconnected trainees from Disconnected elasticsearch index.
   *
   * @param searchQuery query to run
   * @param pageable    pagination information
   */
  public ConnectionSummaryDto searchForPage(String searchQuery, Pageable pageable) {

    try {
      // for each column filter set, place a must between them
      var mustBetweenDifferentColumnFilters = new BoolQueryBuilder();

      //apply free text search on the searchable columns
      BoolQueryBuilder shouldQuery = applyTextBasedSearchQuery(searchQuery.toLowerCase());

      // add the free text query with a must to the column filters query
      BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);

      LOG.debug("Query {}", fullQuery);

      Page<DisconnectedView> result = disconnectedElasticSearchRepository
          .search(fullQuery, pageable);

      final var disconnectedTrainees = result.get().collect(toList());
      return ConnectionSummaryDto.builder()
          .totalPages(result.getTotalPages())
          .totalResults(result.getTotalElements())
          .connections(connectionInfoMapper.disconnectedToDtos(disconnectedTrainees))
          .build();

    } catch (RuntimeException re) {
      LOG.error("An exception occurred while attempting to do an ES search", re);
      throw re;
    }
  }

  /**
   * add new disconnected trainee to elasticsearch index.
   *
   * @param dataToSave disconnected trainee to go in elasticsearch
   */
  public void saveDisconnectedViews(DisconnectedView dataToSave) {
    Iterable<DisconnectedView> existingRecords
        = findDisconnectedViewsByGmcNumberPersonId(dataToSave);

    // if trainee already exists in ES index, then update the existing record
    if (Iterables.size(existingRecords) > 0) {
      updateDisconnectedViews(existingRecords, dataToSave);
    }
    // otherwise, add a new record
    else {
      addDisconnectedViews(dataToSave);
    }
  }

  /**
   * remove disconnected trainee from elasticsearch index by gmcReferenceNumber.
   *
   * @param gmcReferenceNumber id of disconnected trainee to remove
   */
  public void removeDisconnectedViewByGmcNumber(String gmcReferenceNumber) {
    if (gmcReferenceNumber != null) {
      disconnectedElasticSearchRepository.deleteByGmcReferenceNumber(gmcReferenceNumber);
    }
  }

  /**
   * remove disconnected trainee from elasticsearch index by tcsPersonId.
   *
   * @param tcsPersonId id of disconnected trainee to remove
   */
  public void removeDisconnectedViewByTcsPersonId(Long tcsPersonId) {
    if (tcsPersonId != null) {
      disconnectedElasticSearchRepository.deleteByTcsPersonId(tcsPersonId);
    }
  }

  /**
   * find existing disconnected trainee from elasticsearch index by gmcNumber or tcsPersonId.
   *
   * @param dataToSave disconnected trainee to be searched in elasticsearch
   */
  private Iterable<DisconnectedView> findDisconnectedViewsByGmcNumberPersonId(
      DisconnectedView dataToSave) {
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
    return disconnectedElasticSearchRepository.search(fullQuery);
  }

  /**
   * add new disconnected trainee to elasticsearch index.
   *
   * @param dataToSave disconnected trainee to go in elasticsearch
   */
  private void addDisconnectedViews(DisconnectedView dataToSave) {
    disconnectedElasticSearchRepository.save(dataToSave);
  }

  /**
   * update existing disconnected trainee to elasticsearch index.
   *
   * @param existingRecords existing disconnected trainee to be updated in elasticsearch
   * @param dataToSave      new disconnected trainee details to be saved in elasticsearch
   */
  private void updateDisconnectedViews(Iterable<DisconnectedView> existingRecords,
      DisconnectedView dataToSave) {
    existingRecords.forEach(disconnectedView -> {
      dataToSave.setId(disconnectedView.getId());
      disconnectedElasticSearchRepository.save(dataToSave);
    });
  }

  private BoolQueryBuilder applyTextBasedSearchQuery(String searchQuery) {
    // place a should between all of the searchable fields
    var shouldQuery = new BoolQueryBuilder();
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
