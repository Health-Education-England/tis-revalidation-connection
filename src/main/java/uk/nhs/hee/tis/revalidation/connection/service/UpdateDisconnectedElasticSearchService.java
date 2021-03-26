package uk.nhs.hee.tis.revalidation.connection.service;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.util.iterable.Iterables;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.entity.DisconnectedView;
import uk.nhs.hee.tis.revalidation.connection.repository.DisconnectedElasticSearchRepository;

@Slf4j
@Service
public class UpdateDisconnectedElasticSearchService {

  @Autowired
  private DisconnectedElasticSearchRepository disconnectedElasticSearchRepository;

  /**
   * add new disconnected trainee to elasticsearch index.
   *
   * @param dataToSave disconnected trainee to go in elasticsearch
   */
  public void saveDisconnectedViews(DisconnectedView dataToSave) {
    Iterable<DisconnectedView> existingRecords = findDisconnectedViewsByGmcNumberPersonId(dataToSave);

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
    disconnectedElasticSearchRepository.deleteByGmcReferenceNumber(gmcReferenceNumber);
  }

  /**
   * remove disconnected trainee from elasticsearch index by tcsPersonId.
   *
   * @param tcsPersonId id of disconnected trainee to remove
   */
  public void removeDisconnectedViewByTcsPersonId(Long tcsPersonId) {
    disconnectedElasticSearchRepository.deleteByTcsPersonId(tcsPersonId);
  }

  /**
   * find existing disconnected trainee from elasticsearch index by gmcNumber or tcsPersonId.
   *
   * @param dataToSave disconnected trainee to be searched in elasticsearch
   */
  private Iterable<DisconnectedView> findDisconnectedViewsByGmcNumberPersonId(DisconnectedView dataToSave) {
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
   * @param dataToSave new disconnected trainee details to be saved in elasticsearch
   */
  private void updateDisconnectedViews(Iterable<DisconnectedView> existingRecords,
      DisconnectedView dataToSave) {
    existingRecords.forEach(disconnectedView -> {
      dataToSave.setId(disconnectedView.getId());
      disconnectedElasticSearchRepository.save(dataToSave);
    });
  }
}
