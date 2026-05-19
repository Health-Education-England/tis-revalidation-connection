/*
 * The MIT License (MIT)
 *
 * Copyright 2026 Crown Copyright (NHS England)
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

package uk.nhs.hee.tis.revalidation.connection.event.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.connection.event.ConnectionChangedApplicationEvent;
import uk.nhs.hee.tis.revalidation.connection.service.HiddenDiscrepancyService;

/**
 * Listens for ConnectionLog events.
 */
@Component
@Slf4j
public class ConnectionLogApplicationEventListener {

  private final HiddenDiscrepancyService hiddenDiscrepancyService;

  /**
   * Create a listener for ConnectionLog Events.
   *
   * @param hiddenDiscrepancyService the service to manage hidden discrepancies
   */
  public ConnectionLogApplicationEventListener(HiddenDiscrepancyService hiddenDiscrepancyService) {
    this.hiddenDiscrepancyService = hiddenDiscrepancyService;
  }

  /**
   * Handle ConnectionLog change events by showing all hidden discrepancies for the GMC ID.
   *
   * @param event the event containing the connection log information
   */
  @EventListener
  public void handleConnectionChangedEvent(ConnectionChangedApplicationEvent event) {
    String gmcId = event.getConnectionLog().getGmcId();
    log.info("Connection Changed, showing all hidden discrepancies for gmcId: {}", gmcId);
    if(gmcId.isBlank()) {
      log.warn("GMC ID is blank in the connection log, cannot show hidden discrepancies.");
      return;
    }
    hiddenDiscrepancyService.showAllHiddenDiscrepanciesForGmcId(gmcId);
  }
}
