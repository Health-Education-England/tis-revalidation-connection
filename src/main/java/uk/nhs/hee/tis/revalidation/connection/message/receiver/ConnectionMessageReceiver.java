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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.service.ConnectionService;
import uk.nhs.hee.tis.revalidation.connection.service.helper.IndexUpdateHelper;
import uk.nhs.hee.tis.revalidation.connection.service.hydration.ConnectionInfoHydrationService;
import uk.nhs.hee.tis.revalidation.connection.service.index.MasterIndexService;

@Slf4j
@Component
public class ConnectionMessageReceiver implements MessageReceiver<ConnectionInfoDto> {

  private IndexUpdateHelper indexUpdateHelper;

  private MasterIndexService masterIndexService;

  private ConnectionInfoHydrationService connectionInfoHydrationService;

  private ConnectionService connectionService;

  /**
   * Class to handle connection update messages
   *
   * @param indexUpdateHelper class to sort views into correct index
   * @param masterIndexService class to update master index
   * @param connectionInfoHydrationService class to fill in missing fields
   */
  public ConnectionMessageReceiver(
      IndexUpdateHelper indexUpdateHelper,
      MasterIndexService masterIndexService,
      ConnectionInfoHydrationService connectionInfoHydrationService
  ) {
    this.indexUpdateHelper = indexUpdateHelper;
    this.masterIndexService = masterIndexService;
    this.connectionInfoHydrationService = connectionInfoHydrationService;
  }

  /**
   * Handles connection update messages
   *
   * @param message message containing ConnectionInfoDto
   */
  @Override
  public void handleMessage(ConnectionInfoDto message) {
    log.debug("MESSAGE RECEIVED: " + message);
    try {
      ConnectionInfoDto connectionInfo = connectionInfoHydrationService.hydrate(message);
      masterIndexService.updateMasterIndex(connectionInfo);
      indexUpdateHelper.updateElasticSearchIndex(connectionInfo);
    } catch (Exception e) {
      log.info("Exception in receiveMessageUpdate: {}", e.getMessage());
    }
  }
}
