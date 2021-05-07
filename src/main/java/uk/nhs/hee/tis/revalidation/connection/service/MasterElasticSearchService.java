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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.vertx.java.core.json.JsonObject;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.MasterDoctorView;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.MasterElasticSearchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MasterElasticSearchService {

  private static final Logger LOG = LoggerFactory.getLogger(MasterElasticSearchService.class);

  @Autowired
  MasterElasticSearchRepository masterElasticSearchRepository;

  @Autowired
  ConnectionInfoMapper connectionInfoMapper;

  @Value("${spring.elasticsearch.rest.uris}")
  private URL ES_URL;

  private final int PAGE_SIZE = 100;

  /**
   * find all trainee from ES Master Index.
   */
  public List<ConnectionInfoDto> findAllMasterToDto() {
    Iterable<MasterDoctorView> masterList = masterElasticSearchRepository.findAll();
    return connectionInfoMapper.masterToDtos(masterList);
  }

  /**
   * find all trainee from ES Master Index.
   * (with Scroll API to avoid ES index max_result_window excess error)
   */
  @SneakyThrows
  public List<ConnectionInfoDto> findAllScroll() {

    List<ConnectionInfoDto> results = new ArrayList<>();

    RestHighLevelClient client = new RestHighLevelClient(
        RestClient.builder(
            new HttpHost(ES_URL.getHost(), ES_URL.getPort(), ES_URL.getProtocol())));

    try {

      // Initial search
      SearchRequest searchRequest = new SearchRequest("masterdoctorindex");
      SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
      searchSourceBuilder.size(PAGE_SIZE);
      searchRequest.source(searchSourceBuilder);
      searchRequest.scroll("5m");
      SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
      String scrollId = searchResponse.getScrollId();
      SearchHits hits = searchResponse.getHits();
      // add records to results
      results.addAll(hitsToDto(hits));

      // while it is not the end of master index, do scroll search
      while (hits.getHits().length == PAGE_SIZE) {
        // Search with last scrollId
        SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
        scrollRequest.scroll("5m");
        SearchResponse searchScrollResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);

        // clear last scrollId
        ClearScrollRequest request = new ClearScrollRequest();
        request.addScrollId(scrollId);

        // update new scrollId and hits records
        scrollId = searchScrollResponse.getScrollId();
        hits = searchScrollResponse.getHits();
        // add records to results
        results.addAll(hitsToDto(hits));
      }
    }
    catch (Exception e) {
      LOG.error("An exception occurred while getting all data from Master Index with Scroll API", e);
      throw e;
    }
    finally {
      client.close();
    }


    return results;
  }

  @SneakyThrows
  private List<ConnectionInfoDto> hitsToDto(SearchHits hits) {
    final ObjectMapper mapper = new ObjectMapper();
    List<ConnectionInfoDto> results = new ArrayList<>(hits.getHits().length);

    for (SearchHit hit : hits.getHits()) {
      String sourceAsString = hit.getSourceAsString();
      JsonObject json = new JsonObject(sourceAsString);
      MasterDoctorView masterDoctorView = mapper.readValue(json.toString(), MasterDoctorView.class);
      results.add(connectionInfoMapper.masterToDto(masterDoctorView));
    }

    return results;
  }
}
