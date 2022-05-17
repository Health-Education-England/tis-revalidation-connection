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
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.MasterElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.connection.service.ConnectionService;
import uk.nhs.hee.tis.revalidation.connection.service.ElasticSearchIndexUpdateHelper;
import uk.nhs.hee.tis.revalidation.connection.service.MasterElasticSearchService;

@Slf4j
@Component
public class ConnectionMessageReceiver implements MessageReceiver<MasterDoctorView> {

  private ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper;


  private ConnectionService connectionService;

  private ConnectionInfoMapper connectionInfoMapper;

  /**
   * Class to handle connection update messages
   *
   * @param elasticSearchIndexUpdateHelper
   * @param connectionService
   * @param connectionInfoMapper
   */
  public ConnectionMessageReceiver(
      ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper,
      ConnectionService connectionService,
      ConnectionInfoMapper connectionInfoMapper
  ) {
    this.elasticSearchIndexUpdateHelper = elasticSearchIndexUpdateHelper;
    this.connectionService = connectionService;
    this.connectionInfoMapper = connectionInfoMapper;
  }

  /**
   * Handles connection update messages
   *
   * @param message message containing updated MasterDoctorView
   */
  @Override
  public void handleMessage(MasterDoctorView message) {
    log.debug("MESSAGE RECEIVED: " + message);

    // Get Gmc data from master index and aggregate it to connectionInfo
    final var update = connectionInfoMapper.masterToDto(message);
    elasticSearchIndexUpdateHelper.updateElasticSearchIndex(update);
  }
}
