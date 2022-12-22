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

package uk.nhs.hee.tis.revalidation.connection.message.listener;

import static java.time.LocalDate.now;
import static org.mockito.Mockito.verify;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.connection.entity.GmcDoctor;
import uk.nhs.hee.tis.revalidation.connection.entity.MasterDoctorView;
import uk.nhs.hee.tis.revalidation.connection.message.receiver.DoctorMessageReceiver;

@ExtendWith(MockitoExtension.class)
class RabbitMessageListenerTest {

  @InjectMocks
  RabbitMessageListener rabbitMessageListener;
  @Mock
  DoctorMessageReceiver doctorMessageReceiver;

  private GmcDoctor gmcDoctor;
  private MasterDoctorView masterDoctorView;
  private Faker faker = new Faker();
  private String gmcRef1;
  private String firstName1;
  private String lastName1;
  private LocalDate submissionDate1;
  private String designatedBody1;
  private String programmeName1;
  private String programmeOwner1;
  private String status;
  private String exceptionReason;

  @BeforeEach
  public void setup() {
    gmcRef1 = faker.number().digits(8);
    firstName1 = faker.name().firstName();
    lastName1 = faker.name().lastName();
    submissionDate1 = now();
    designatedBody1 = faker.lorem().characters(8);
    programmeName1 = faker.lorem().characters(20);
    programmeOwner1 = faker.lorem().characters(20);
    status = faker.lorem().characters(8);
    gmcDoctor = buildGmcDoctor();
    masterDoctorView = buildMasterDoctorView();
    exceptionReason = faker.lorem().characters(20);
  }

  @Test
  void shouldReceiveDoctorMessages() {
    rabbitMessageListener.receiveDoctorMessage(masterDoctorView);
    verify(doctorMessageReceiver).handleMessage(masterDoctorView);
  }

  private GmcDoctor buildGmcDoctor() {
    return GmcDoctor.builder()
        .gmcReferenceNumber(gmcRef1)
        .doctorFirstName(firstName1)
        .doctorLastName(lastName1)
        .submissionDate(submissionDate1.toString())
        .doctorStatus(status)
        .breach(faker.lorem().characters(20))
        .dateAdded(faker.date().past(10, TimeUnit.DAYS).toString())
        .underNotice(faker.lorem().characters(5))
        .investigation(faker.lorem().characters(20))
        .preliminaryInvestigation(faker.lorem().characters(20))
        .sanction(faker.lorem().characters(10))
        .designatedBodyCode(designatedBody1)
        .build();
  }

  private MasterDoctorView buildMasterDoctorView() {
    return MasterDoctorView.builder()
        .tcsPersonId((long) 111)
        .gmcReferenceNumber(gmcRef1)
        .doctorFirstName(firstName1)
        .doctorLastName(lastName1)
        .submissionDate(submissionDate1)
        .programmeName(programmeName1)
        .designatedBody(designatedBody1)
        .programmeOwner(programmeOwner1)
        .connectionStatus(status)
        .exceptionReason(exceptionReason)
        .build();
  }
}
