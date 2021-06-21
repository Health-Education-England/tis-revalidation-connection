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
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.MasterDoctorView;
import uk.nhs.hee.tis.revalidation.connection.repository.MasterElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.connection.service.ConnectionService;
import uk.nhs.hee.tis.revalidation.connection.service.ElasticSearchIndexUpdateHelper;
import uk.nhs.hee.tis.revalidation.connection.service.MasterElasticSearchService;

@Slf4j
@Component
public class ConnectionMessageReceiver implements MessageReceiver<ConnectionInfoDto> {

  private ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper;

  private MasterElasticSearchService masterElasticSearchService;

  private MasterElasticSearchRepository masterElasticSearchRepository;

  private ConnectionService connectionService;

  /**
   * Class to handle connection update messages
   *
   * @param elasticSearchIndexUpdateHelper
   * @param masterElasticSearchService
   * @param connectionService
   */
  public ConnectionMessageReceiver(
      ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper,
      MasterElasticSearchService masterElasticSearchService,
      ConnectionService connectionService,
      MasterElasticSearchRepository masterElasticSearchRepository
  ) {
    this.elasticSearchIndexUpdateHelper = elasticSearchIndexUpdateHelper;
    this.masterElasticSearchService = masterElasticSearchService;
    this.connectionService = connectionService;
    this.masterElasticSearchRepository = masterElasticSearchRepository;
  }

  /**
   * Handles connection update messages
   *
   * @param message message containing ConnectionInfoDto
   */
  @Override
  public void handleMessage(ConnectionInfoDto message) {
    log.debug("MESSAGE RECEIVED: " + message);

    // Get Gmc data from master index and aggregate it to connectionInfo
    try {
      if (message.getGmcReferenceNumber() != null) {
        List<MasterDoctorView> gmcData = masterElasticSearchRepository
            .findByGmcReferenceNumber(message.getGmcReferenceNumber());
        if (gmcData.size() > 0) {
          MasterDoctorView gmcDataMasterDoctor = gmcData.get(0);
          message.setSubmissionDate(gmcDataMasterDoctor.getSubmissionDate());
          message.setDesignatedBody(gmcDataMasterDoctor.getDesignatedBody());
        }
      }
      masterElasticSearchService.updateMasterIndex(message);
      elasticSearchIndexUpdateHelper.updateElasticSearchIndex(message);
    } catch (Exception e) {
      log.info("Exception in receiveMessageUpdate: {}", e.getMessage());
    }
  }
}
