package uk.nhs.hee.tis.revalidation.connection.service;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.util.iterable.Iterables;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionView;
import uk.nhs.hee.tis.revalidation.connection.repository.ExceptionElasticSearchRepository;

@Slf4j
@Service
public class UpdateExceptionElasticSearchService {

  @Autowired
  private ExceptionElasticSearchRepository exceptionElasticSearchRepository;

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
    exceptionElasticSearchRepository.deleteByGmcReferenceNumber(gmcReferenceNumber);
  }

  /**
   * remove exceptions from elasticsearch index by tcsPersonId.
   *
   * @param tcsPersonId id of exception to remove
   */
  public void removeExceptionViewByTcsPersonId(Long tcsPersonId) {
    exceptionElasticSearchRepository.deleteByTcsPersonId(tcsPersonId);
  }

  /**
   * find existing exceptions from elasticsearch index by gmcNumber or tcsPersonId.
   *
   * @param dataToSave exception to be searched in elasticsearch
   */
  private Iterable<ExceptionView> findExceptionViewsByGmcNumberPersonId(ExceptionView dataToSave) {
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
    return exceptionElasticSearchRepository.search(fullQuery);
  }

  /**
   * add new exceptions to elasticsearch index.
   *
   * @param dataToSave exceptions to go in elasticsearch
   */
  private void addExceptionViews(ExceptionView dataToSave) {
    exceptionElasticSearchRepository.save(dataToSave);
  }

  /**
   * update existing exceptions to elasticsearch index.
   *
   * @param existingRecords existing exceptions to be updated in elasticsearch
   * @param dataToSave new exceptions details to be saved in elasticsearch
   */
  private void updateExceptionViews(Iterable<ExceptionView> existingRecords,
      ExceptionView dataToSave) {
    existingRecords.forEach(exceptionView -> {
      dataToSave.setId(exceptionView.getId());
      exceptionElasticSearchRepository.save(dataToSave);
    });
  }
}
