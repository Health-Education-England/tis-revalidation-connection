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

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.util.iterable.Iterables;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectedView;
import uk.nhs.hee.tis.revalidation.connection.repository.ConnectedElasticSearchRepository;

@Slf4j
@Service
public class UpdateConnectedElasticSearchService {

  @Autowired
  private ConnectedElasticSearchRepository connectedElasticSearchRepository;

  /**
   * add new connected trainee to elasticsearch index.
   *
   * @param dataToSave connected trainee to go in elasticsearch
   */
  public void saveConnectedViews(ConnectedView dataToSave) {
    Iterable<ConnectedView> existingRecords = findConnectedViewsByGmcNumberPersonId(dataToSave);

    // if trainee already exists in ES index, then update the existing record
    if (Iterables.size(existingRecords) > 0) {
      updateConnectedViews(existingRecords, dataToSave);
    }
    // otherwise, add a new record
    else {
      addConnectedViews(dataToSave);
    }
  }

  /**
   * remove connected trainee from elasticsearch index by gmcReferenceNumber.
   *
   * @param gmcReferenceNumber id of connected trainee to remove
   */
  public void removeConnectedViewByGmcNumber(String gmcReferenceNumber) {
    if (gmcReferenceNumber != null) {
      connectedElasticSearchRepository.deleteByGmcReferenceNumber(gmcReferenceNumber);
    }
  }

  /**
   * remove connected trainee from elasticsearch index by tcsPersonId.
   *
   * @param tcsPersonId id of connected trainee to remove
   */
  public void removeConnectedViewByTcsPersonId(Long tcsPersonId) {
    if (tcsPersonId != null) {
      connectedElasticSearchRepository.deleteByTcsPersonId(tcsPersonId);
    }
  }

  /**
   * find existing connected trainee from elasticsearch index by gmcNumber or tcsPersonId.
   *
   * @param dataToSave connected trainee to be searched in elasticsearch
   */
  private Iterable<ConnectedView> findConnectedViewsByGmcNumberPersonId(ConnectedView dataToSave) {
    BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();
    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();

    if (dataToSave.getGmcReferenceNumber() != null) {
      shouldQuery
          .should(new MatchQueryBuilder("gmcReferenceNumber", dataToSave.getGmcReferenceNumber()));
    }
    if (dataToSave.getTcsPersonId() != null) {
      shouldQuery
          .should(new MatchQueryBuilder("tcsPersonId", dataToSave.getTcsPersonId()));
    }

    BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);
    return connectedElasticSearchRepository.search(fullQuery);
  }

  /**
   * add new connected trainee to elasticsearch index.
   *
   * @param dataToSave connected trainee to go in elasticsearch
   */
  private void addConnectedViews(ConnectedView dataToSave) {
    connectedElasticSearchRepository.save(dataToSave);
  }

  /**
   * update existing connecteds to elasticsearch index.
   *
   * @param existingRecords existing connecteds to be updated in elasticsearch
   * @param dataToSave      new connecteds details to be saved in elasticsearch
   */
  private void updateConnectedViews(Iterable<ConnectedView> existingRecords,
      ConnectedView dataToSave) {
    existingRecords.forEach(connectedView -> {
      dataToSave.setId(connectedView.getId());
      connectedElasticSearchRepository.save(dataToSave);
    });
  }
}
