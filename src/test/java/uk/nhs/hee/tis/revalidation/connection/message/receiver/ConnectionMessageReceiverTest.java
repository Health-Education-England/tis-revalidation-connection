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

import static java.time.LocalDate.now;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.service.helper.IndexUpdateHelper;
import uk.nhs.hee.tis.revalidation.connection.service.hydration.ConnectionInfoHydrationService;
import uk.nhs.hee.tis.revalidation.connection.service.index.MasterIndexService;

@ExtendWith(MockitoExtension.class)
public class ConnectionMessageReceiverTest {
  @InjectMocks
  private ConnectionMessageReceiver connectionMessageReceiver;
  @Mock
  private IndexUpdateHelper indexUpdateHelper;
  @Mock
  private MasterIndexService masterIndexService;
  @Mock
  private ConnectionInfoHydrationService connectionInfoHydrationService;

  private Faker faker = new Faker();
  private String gmcRef1;
  private String firstName1;
  private String lastName1;
  private LocalDate submissionDate1;
  private String designatedBody1;
  private String programmeName1;
  private String programmeOwner1;
  ConnectionInfoDto messageDto;
  ConnectionInfoDto hydratedDto;

  /**
   * Set up data for testing.
   */
  @BeforeEach
  public void setup() {
    initializeFields();
    initializeMessageDto();
    initializeHydratedDto();
  }

  @Test
  void shouldUpdateMasterIndexAndOtherIndexesOnReceiveConnectionInfo() {
    when(connectionInfoHydrationService.hydrate(messageDto)).thenReturn(hydratedDto);
    connectionMessageReceiver.handleMessage(messageDto);
    verify(masterIndexService).updateMasterIndex(hydratedDto);
    verify(indexUpdateHelper).updateElasticSearchIndex(hydratedDto);
  }

  private void initializeFields() {
    gmcRef1 = faker.number().digits(8);
    firstName1 = faker.name().firstName();
    lastName1 = faker.name().lastName();
    submissionDate1 = now();
    designatedBody1 = faker.lorem().characters(8);
    programmeName1 = faker.lorem().characters(20);
    programmeOwner1 = faker.lorem().characters(20);
  }

  private void initializeMessageDto() {
    messageDto = ConnectionInfoDto.builder()
        .tcsPersonId((long) 111)
        .gmcReferenceNumber(gmcRef1)
        .doctorFirstName(firstName1)
        .doctorLastName(lastName1)
        .submissionDate(null)
        .programmeName(programmeName1)
        .designatedBody(null)
        .programmeOwner(programmeOwner1)
        .build();
  }

  private void initializeHydratedDto () {
    hydratedDto = ConnectionInfoDto.builder()
        .tcsPersonId((long) 111)
        .gmcReferenceNumber(gmcRef1)
        .doctorFirstName(firstName1)
        .doctorLastName(lastName1)
        .submissionDate(submissionDate1)
        .programmeName(programmeName1)
        .designatedBody(designatedBody1)
        .programmeOwner(programmeOwner1)
        .build();
  }

}
