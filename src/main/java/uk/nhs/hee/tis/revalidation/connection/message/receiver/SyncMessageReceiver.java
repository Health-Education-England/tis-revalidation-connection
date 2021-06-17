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

package uk.nhs.hee.tis.revalidation.connection.message.receiver;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.connection.config.ElasticsearchConfig;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.service.ElasticSearchIndexUpdateHelper;
import uk.nhs.hee.tis.revalidation.connection.service.MasterElasticSearchService;

@Slf4j
@Component
public class SyncMessageReceiver implements MessageReceiver<String> {

  private ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper;

  private MasterElasticSearchService masterElasticSearchService;

  /**
   * Class to handle connection update messages
   *
   * @param elasticSearchIndexUpdateHelper
   * @param masterElasticSearchService
   */
  public SyncMessageReceiver(
      ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper,
      MasterElasticSearchService masterElasticSearchService
  ) {
    this.elasticSearchIndexUpdateHelper = elasticSearchIndexUpdateHelper;
    this.masterElasticSearchService = masterElasticSearchService;
  }

  /**
   * Handles sync process messages
   *
   * @param message sync instruction String
   */
  @Override
  public void handleMessage(final String message) {
    if (message != null && message.equals("getMaster")) {

      //Delete and create elastic search index
      elasticSearchIndexUpdateHelper.clearConnectionIndexes(ElasticsearchConfig.ES_INDICES);

      final List<ConnectionInfoDto> masterList = masterElasticSearchService.findAllScroll();
      log.info("Found {} records from ES Master index. ", masterList.size());

      masterList.forEach(connectionInfo ->
          elasticSearchIndexUpdateHelper.updateElasticSearchIndex(connectionInfo));
      log.info("ES indexes update completed.");
    }
  }
}
