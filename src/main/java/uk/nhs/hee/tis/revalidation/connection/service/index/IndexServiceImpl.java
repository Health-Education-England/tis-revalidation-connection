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

package uk.nhs.hee.tis.revalidation.connection.service.index;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.util.iterable.Iterables;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionSummaryDto;
import uk.nhs.hee.tis.revalidation.connection.entity.BaseConnectionView;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapperHelper;
import uk.nhs.hee.tis.revalidation.connection.repository.index.IndexRepository;

@Slf4j
@Service
public class IndexServiceImpl<T extends BaseConnectionView> implements IndexService<T> {

  IndexRepository<T> indexRepository;

  ConnectionInfoMapperHelper connectionInfoMapper;

  public IndexServiceImpl(
      IndexRepository<T> indexRepository,
      ConnectionInfoMapperHelper connectionInfoMapper
  ) {
    this.indexRepository = indexRepository;
    this.connectionInfoMapper = connectionInfoMapper;
  }

  /**
   * Get trainee views from index.
   *
   * @param searchQuery query to run
   * @param pageable    pagination information
   */
  @Override
  public ConnectionSummaryDto searchForPage(String searchQuery, Pageable pageable) {
    try {
      // for each column filter set, place a must between them
      var mustBetweenDifferentColumnFilters = new BoolQueryBuilder();

      //apply free text search on the searchable columns
      BoolQueryBuilder shouldQuery = applyTextBasedSearchQuery(searchQuery.toLowerCase());

      // add the free text query with a must to the column filters query
      BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);

      log.debug("Query {}", fullQuery);

      Page<T> result = indexRepository.executePagedQuery(fullQuery, pageable);
      List<T> connectedTrainees = result.get().collect(toList());
      return ConnectionSummaryDto.builder()
          .totalPages(result.getTotalPages())
          .totalResults(result.getTotalElements())
          .connections(connectionInfoMapper.toDtos(connectedTrainees))
          .build();

    } catch (RuntimeException re) {
      log.error("An exception occurred while attempting to do an index search",
          re);
      throw re;
    }
  }

  /**
   * add new trainee view to index.
   *
   * @param dataToSave trainee view to go in index
   */
  @Override
  public void saveView(T dataToSave) {
    Iterable<T> existingRecords = findViewsByGmcNumberPersonId(dataToSave);

    // if trainee already exists in ES index, then update the existing record
    if (Iterables.size(existingRecords) > 0) {
      updateViews(existingRecords, dataToSave);
    }
    // otherwise, add a new record
    else {
      indexRepository.save(dataToSave);
    }
  }

  /**
   * remove trainee view from index by gmcReferenceNumber.
   *
   * @param gmcReferenceNumber id of trainee view to remove
   */
  @Override
  public void removeViewByGmcReferenceNumber(String gmcReferenceNumber) {
    if (gmcReferenceNumber != null) {
      try {
        indexRepository.deleteViewByGmcReferenceNumber(gmcReferenceNumber);
      }
      catch (Exception ex) {
        log.info("Exception in `removeViewByGmcReferenceNumber` (GmcId: {}): {}",
            gmcReferenceNumber,  ex);
      }
    }
  }

  /**
   * remove trainee view from index by tcsPersonId.
   *
   * @param tcsPersonId id of trainee view to remove
   */
  @Override
  public void removeViewByTcsPersonId(Long tcsPersonId) {
    if (tcsPersonId != null) {
      try {
        indexRepository.deleteViewByTcsPersonId(tcsPersonId);
      }
      catch (Exception ex) {
        log.info("Exception in `removeViewByTcsPersonId` (PersonId: {}): {}",
            tcsPersonId,  ex);
      }
    }
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

  /**
   * update existing connection views to index.
   *
   * @param existingRecords existing views to be updated in index
   * @param dataToSave      new views details to be saved in index
   */
  private void updateViews(Iterable<T> existingRecords,
                                    T dataToSave) {
    existingRecords.forEach(connectedView -> {
      dataToSave.setId(connectedView.getId());
      try {
        indexRepository.save(dataToSave);
      }
      catch (Exception ex) {
        log.info("Exception in `updateViews` (GmcId: {}; PersonId: {}): {}",
            dataToSave.getGmcReferenceNumber(),dataToSave.getTcsPersonId(),  ex);
      }
    });
  }

  /**
   * find existing connected trainee from elasticsearch index by gmcNumber or tcsPersonId.
   *
   * @param dataToSave connected trainee to be searched in elasticsearch
   */
  private Iterable<T> findViewsByGmcNumberPersonId(T dataToSave) {
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
      result = indexRepository.executeQuery(fullQuery);
    }
    catch (Exception ex) {
      log.info("Exception in `findViewsByGmcNumberPersonId` (GmcId: {}; PersonId: {}): {}",
          dataToSave.getGmcReferenceNumber(),dataToSave.getTcsPersonId(),  ex);
    }

    return result;
  }
}
