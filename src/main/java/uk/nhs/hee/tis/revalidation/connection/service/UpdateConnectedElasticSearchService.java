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
   * @param dataToSave new connecteds details to be saved in elasticsearch
   */
  private void updateConnectedViews(Iterable<ConnectedView> existingRecords,
      ConnectedView dataToSave) {
    existingRecords.forEach(connectedView -> {
      dataToSave.setId(connectedView.getId());
      connectedElasticSearchRepository.save(dataToSave);
    });
  }
}
