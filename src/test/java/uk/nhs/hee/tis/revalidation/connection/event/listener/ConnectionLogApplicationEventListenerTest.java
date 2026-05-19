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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionLog;
import uk.nhs.hee.tis.revalidation.connection.event.ConnectionChangedApplicationEvent;
import uk.nhs.hee.tis.revalidation.connection.service.HiddenDiscrepancyService;

@ExtendWith(MockitoExtension.class)
class ConnectionLogApplicationEventListenerTest {

  private static final String GMC_ID_1 = "1234567";
  private static final String DBC_1 = "1-ABCDE";
  private static final String DBC_2 = "1-FGHIJ";
  private static final String UPDATED_BY = "admin";
  private static final String CONNECTION_LOG_ID = "connection-log-1";

  @Mock
  private HiddenDiscrepancyService hiddenDiscrepancyService;

  @InjectMocks
  private ConnectionLogApplicationEventListener listener;

  private ConnectionLog connectionLog;

  @BeforeEach
  void setUp() {
    connectionLog = ConnectionLog.builder()
        .id(CONNECTION_LOG_ID)
        .gmcId(GMC_ID_1)
        .newDesignatedBodyCode(DBC_1)
        .previousDesignatedBodyCode(DBC_2)
        .updatedBy(UPDATED_BY)
        .requestTime(LocalDateTime.now())
        .build();
  }

  @Test
  void shouldShowAllHiddenDiscrepanciesWhenConnectionChanged() {
    ConnectionChangedApplicationEvent event = new ConnectionChangedApplicationEvent(connectionLog);

    listener.handleConnectionChangedEvent(event);

    verify(hiddenDiscrepancyService).showAllHiddenDiscrepanciesForGmcId(GMC_ID_1);
  }

  @Test
  void shouldNotShowHiddenDiscrepanciesWhenGmcIdIsNull() {
    connectionLog.setGmcId(null);
    ConnectionChangedApplicationEvent event = new ConnectionChangedApplicationEvent(connectionLog);

    listener.handleConnectionChangedEvent(event);

    verifyNoInteractions(hiddenDiscrepancyService);
  }

  @Test
  void shouldHandleEmptyStringGmcId() {
    connectionLog.setGmcId("");
    ConnectionChangedApplicationEvent event = new ConnectionChangedApplicationEvent(connectionLog);

    listener.handleConnectionChangedEvent(event);

    verify(hiddenDiscrepancyService).showAllHiddenDiscrepanciesForGmcId("");
  }
}

