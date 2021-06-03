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


import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.common.util.iterable.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchScrollHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.config.ElasticSearchConfig;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionSummaryDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectedView;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.ConnectedElasticSearchRepository;

import java.util.ArrayList;
import java.util.List;


@Service
public class ConnectedElasticSearchService extends AbstractElasticSearchService<ConnectedView> {

  private static final Logger LOG = LoggerFactory.getLogger(ConnectedElasticSearchService.class);

  ConnectedElasticSearchRepository repository;

  ConnectionInfoMapper mapper;

  private ElasticsearchRestTemplate elasticsearchTemplate;

  public ConnectedElasticSearchService(
      ConnectedElasticSearchRepository repository,
      ConnectionInfoMapper mapper,
      ElasticsearchRestTemplate elasticsearchTemplate) {
    super(repository, mapper);
    this.repository = repository;
    this.mapper = mapper;
    this.elasticsearchTemplate = elasticsearchTemplate;
  }

  @Override
  public ConnectionSummaryDto searchForPage(String searchQuery, Pageable pageable) {
    try {
      var objectMapper = new ObjectMapper();
      List<ConnectedView> views = new ArrayList<>();
      List<String> scrollIds = new ArrayList<>();

      // initial search
      var query = new NativeSearchQueryBuilder().withQuery(applyTextBasedSearchQuery(searchQuery)).build();
      SearchScrollHits<ConnectedView> scroll = elasticsearchTemplate
          .searchScrollStart(
              ElasticSearchConfig.SCROLL_TIMEOUT_MS,
              query,
              ConnectedView.class,
              ElasticSearchConfig.CONNECTED_INDEX
          );

      // while it is not the end of data
      while (scroll.hasSearchHits()) {
        // convert and store data to list
        for (SearchHit hit : scroll.getSearchHits()) {
          var view = objectMapper.convertValue(hit.getContent(), ConnectedView.class);
          views.add(view);
        }

        // do search scroll with last scrollId
        String scrollId = scroll.getScrollId();
        scroll = elasticsearchTemplate
            .searchScrollContinue(
                scrollId,
                ElasticSearchConfig.SCROLL_TIMEOUT_MS,
                ConnectedView.class,
                ElasticSearchConfig.CONNECTED_INDEX
            );
        scrollIds.add(scrollId);
      }
      elasticsearchTemplate.searchScrollClear(scrollIds);
      Page<ConnectedView> result = new PageImpl<>(views, pageable, views.size());

      return ConnectionSummaryDto.builder()
          .totalPages(result.getTotalPages())
          .totalResults(result.getTotalElements())
          .connections(mapper.connectedToDtos(views))
          .build();

    } catch (RuntimeException re) {
      LOG.error("An exception occurred while attempting to do an ES search - Connected index",
          re);
      throw re;
  }

  }

  /**
   * add new connected trainee to elasticsearch index.
   *
   * @param dataToSave connected trainee to go in elasticsearch
   */
  public void saveViews(ConnectedView dataToSave) {
    Iterable<ConnectedView> existingRecords = findViewsByGmcNumberPersonId(dataToSave);

    // if trainee already exists in ES index, then update the existing record
    if (Iterables.size(existingRecords) > 0) {
      updateViews(existingRecords, dataToSave);
    }
    // otherwise, add a new record
    else {
      addViews(dataToSave);
    }
  }

  /**
   * remove connected trainee from elasticsearch index by gmcReferenceNumber.
   *
   * @param gmcReferenceNumber id of connected trainee to remove
   */
  public void removeViewByGmcNumber(String gmcReferenceNumber) {
    if (gmcReferenceNumber != null) {
      try {
        repository.deleteByGmcReferenceNumber(gmcReferenceNumber);
      }
      catch (Exception ex) {
        LOG.info("Exception in `removeConnectedViewByGmcNumber` (GmcId: {}): {}",
            gmcReferenceNumber,  ex);
      }
    }
  }

  /**
   * remove connected trainee from elasticsearch index by tcsPersonId.
   *
   * @param tcsPersonId id of connected trainee to remove
   */
  public void removeViewByTcsPersonId(Long tcsPersonId) {
    if (tcsPersonId != null) {
      try {
        repository.deleteByTcsPersonId(tcsPersonId);
      }
      catch (Exception ex) {
        LOG.info("Exception in `removeConnectedViewByTcsPersonId` (PersonId: {}): {}",
            tcsPersonId,  ex);
      }
    }
  }

}
