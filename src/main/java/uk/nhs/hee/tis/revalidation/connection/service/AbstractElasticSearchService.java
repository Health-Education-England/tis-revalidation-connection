package uk.nhs.hee.tis.revalidation.connection.service;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionSummaryDto;
import uk.nhs.hee.tis.revalidation.connection.entity.BaseConnectionView;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;

import java.util.ArrayList;

public abstract class AbstractElasticSearchService<T extends BaseConnectionView> {

  private static final Logger LOG = LoggerFactory.getLogger(ConnectedElasticSearchService.class);

  ElasticsearchRepository repository;

  ConnectionInfoMapper mapper;

  public AbstractElasticSearchService(
      ElasticsearchRepository repository,
      ConnectionInfoMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }


  /**
   * find existing  trainee from elasticsearch index by gmcNumber or tcsPersonId.
   *
   * @param dataToSave  trainee to be searched in elasticsearch
   */
  Iterable<T> findViewsByGmcNumberPersonId(T dataToSave) {
    Iterable<T> result = new ArrayList<>();
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
      result = repository.search(fullQuery);
    }
    catch (Exception ex) {
      LOG.info("Exception in `findViewsByGmcNumberPersonId` (GmcId: {}; PersonId: {}): {}",
          dataToSave.getGmcReferenceNumber(),dataToSave.getTcsPersonId(),  ex);
    }

    return result;
  }

  /**
   * add new trainee to elasticsearch index.
   *
   * @param dataToSave trainee to go in elasticsearch
   */
  void addViews(T dataToSave) {
    try {
      repository.save(dataToSave);
    }
    catch (Exception ex) {
      LOG.info("Exception in `addViews` (GmcId: {}; PersonId: {}): {}",
          dataToSave.getGmcReferenceNumber(),dataToSave.getTcsPersonId(),  ex);
    }
  }

  /**
   * update existing trainees to elasticsearch index.
   *
   * @param existingRecords existing trainees to be updated in elasticsearch
   * @param dataToSave      new trainees details to be saved in elasticsearch
   */
  void updateViews(Iterable<T> existingRecords,
                                    T dataToSave) {
    existingRecords.forEach(view -> {
      dataToSave.setId(view.getId());
      try {
        repository.save(dataToSave);
      }
      catch (Exception ex) {
        LOG.info("Exception in `updateViews` (GmcId: {}; PersonId: {}): {}",
            dataToSave.getGmcReferenceNumber(),dataToSave.getTcsPersonId(),  ex);
      }
    });
  }

  BoolQueryBuilder applyTextBasedSearchQuery(String searchQuery) {
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

  /**
   * Get trainees from elasticsearch index.
   *
   * @param searchQuery query to run
   * @param pageable    pagination information
   */
  public abstract ConnectionSummaryDto searchForPage(String searchQuery, Pageable pageable);

  /**
   * add new trainee to elasticsearch index.
   *
   * @param dataToSave trainee to go in elasticsearch
   */
  public abstract void saveViews(T dataToSave);

  /**
   * remove trainee from elasticsearch index by gmcReferenceNumber.
   *
   * @param gmcReferenceNumber id of connected trainee to remove
   */
  public abstract void removeViewByGmcNumber(String gmcReferenceNumber);

  /**
   * remove trainee from elasticsearch index by tcsPersonId.
   *
   * @param tcsPersonId id of trainee to remove
   */
  public abstract void removeViewByTcsPersonId(Long tcsPersonId);

}
