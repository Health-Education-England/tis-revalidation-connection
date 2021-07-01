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
import static uk.nhs.hee.tis.revalidation.connection.entity.GmcResponseCode.SUCCESS;
import static uk.nhs.hee.tis.revalidation.connection.entity.GmcResponseCode.INVALID_CREDENTIALS;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.MasterDoctorView;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapperImpl;
import uk.nhs.hee.tis.revalidation.connection.repository.MasterElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.connection.service.ConnectionService;
import uk.nhs.hee.tis.revalidation.connection.service.ElasticSearchIndexUpdateHelper;
import uk.nhs.hee.tis.revalidation.connection.service.MasterElasticSearchService;

@ExtendWith(MockitoExtension.class)
public class UpdateConnectionReceiverTest {

  Faker faker = new Faker();

  UpdateConnectionReceiver updateConnectionReceiver;
  @Mock
  ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper;
  @Mock
  MasterElasticSearchService masterElasticSearchService;
  @Mock
  MasterElasticSearchRepository masterElasticSearchRepository;
  ConnectionInfoMapper connectionInfoMapper;

  private ConnectionService connectionService;
  private String gmcRef;
  private String firstName;
  private String lastName;
  private LocalDate submissionDate;
  private String designatedBody;
  private String programmeName;
  private String programmeOwner;
  private String status;
  private String designatedBody1;
  ConnectionInfoDto existingTrainee;
  ConnectionInfoDto connectionInfoDtoAdd;
  ConnectionInfoDto connectionInfoDtoAddResult;
  ConnectionInfoDto connectionInfoDtoRemove;
  ConnectionInfoDto connectionInfoDtoRemoveResult;
  ConnectionInfoDto connectionInfoDtoException;
  ConnectionInfoDto connectionInfoDtoExceptionResult;
  private List<MasterDoctorView> masterDoctorViews = new ArrayList<>();

  @BeforeEach
  public void setup() {
    connectionInfoMapper = new ConnectionInfoMapperImpl();

    updateConnectionReceiver = new UpdateConnectionReceiver(
        elasticSearchIndexUpdateHelper,
        masterElasticSearchService,
        masterElasticSearchRepository,
        connectionInfoMapper
    );
    gmcRef = faker.number().digits(8);
    firstName = faker.name().firstName();
    lastName = faker.name().lastName();
    submissionDate = now();
    designatedBody = faker.lorem().characters(8);
    designatedBody1 = faker.lorem().characters(8);
    programmeName = faker.lorem().characters(20);
    programmeOwner = faker.lorem().characters(20);
    status = faker.lorem().characters(8);

    existingTrainee = buildExistingTrainee();

    connectionInfoDtoAdd = buildConnectionInfoDtoAdd();
    connectionInfoDtoAddResult = buildConnectionInfoDtoAddResult();

    connectionInfoDtoRemove = buildConnectionInfoDtoRemove();
    connectionInfoDtoRemoveResult = buildConnectionInfoDtoRemoveResult();

    connectionInfoDtoException = buildConnectionInfoDtoException();
    connectionInfoDtoExceptionResult = buildConnectionInfoDtoExceptionResult();

    masterDoctorViews.add(connectionInfoMapper.dtoToMaster(existingTrainee));

  }

  @Test
  void shouldAddConnectionsOnReceiveMessage() {
    when(masterElasticSearchRepository.findByGmcReferenceNumber(gmcRef))
        .thenReturn(masterDoctorViews);
    updateConnectionReceiver.handleMessage(connectionInfoDtoAdd);
    verify(elasticSearchIndexUpdateHelper).updateElasticSearchIndex(connectionInfoDtoAddResult);
    verify(masterElasticSearchService).updateMasterIndex(connectionInfoDtoAddResult);
  }

  @Test
  void shouldRemoveConnectionsOnReceiveMessage() {
    when(masterElasticSearchRepository.findByGmcReferenceNumber(gmcRef))
        .thenReturn(masterDoctorViews);
    updateConnectionReceiver.handleMessage(connectionInfoDtoRemove);
    verify(elasticSearchIndexUpdateHelper).updateElasticSearchIndex(connectionInfoDtoRemoveResult);
    verify(masterElasticSearchService).updateMasterIndex(connectionInfoDtoRemoveResult);
  }

  @Test
  void shouldHandleExceptionOnReceiveMessage() {
    when(masterElasticSearchRepository.findByGmcReferenceNumber(gmcRef))
        .thenReturn(masterDoctorViews);
    updateConnectionReceiver.handleMessage(connectionInfoDtoException);
    verify(elasticSearchIndexUpdateHelper).updateElasticSearchIndex(connectionInfoDtoExceptionResult);
    verify(masterElasticSearchService).updateMasterIndex(connectionInfoDtoExceptionResult);
  }

  private ConnectionInfoDto buildExistingTrainee() {
    return ConnectionInfoDto.builder()
        .tcsPersonId((long) 111)
        .gmcReferenceNumber(gmcRef)
        .doctorFirstName(firstName)
        .doctorLastName(lastName)
        .submissionDate(submissionDate)
        .programmeName(programmeName)
        .designatedBody(designatedBody)
        .programmeOwner(programmeOwner)
        .connectionStatus(status)
        .exceptionReason(null)
        .build();
  }

  private ConnectionInfoDto buildConnectionInfoDtoAdd() {
    return ConnectionInfoDto.builder()
        .gmcReferenceNumber(gmcRef)
        .designatedBody(designatedBody1)
        .exceptionReason(SUCCESS.getMessage())
        .build();
  }

  private ConnectionInfoDto buildConnectionInfoDtoAddResult() {
    return ConnectionInfoDto.builder()
        .tcsPersonId((long) 111)
        .gmcReferenceNumber(gmcRef)
        .doctorFirstName(firstName)
        .doctorLastName(lastName)
        .submissionDate(submissionDate)
        .programmeName(programmeName)
        .designatedBody(designatedBody1)
        .programmeOwner(programmeOwner)
        .connectionStatus("Yes")
        .exceptionReason(null)
        .build();
  }

  private ConnectionInfoDto buildConnectionInfoDtoRemove() {
    return ConnectionInfoDto.builder()
        .gmcReferenceNumber(gmcRef)
        .designatedBody(null)
        .exceptionReason(SUCCESS.getMessage())
        .build();
  }

  private ConnectionInfoDto buildConnectionInfoDtoRemoveResult() {
    return ConnectionInfoDto.builder()
        .tcsPersonId((long) 111)
        .gmcReferenceNumber(gmcRef)
        .doctorFirstName(firstName)
        .doctorLastName(lastName)
        .submissionDate(submissionDate)
        .programmeName(programmeName)
        .designatedBody(null)
        .programmeOwner(programmeOwner)
        .connectionStatus("No")
        .exceptionReason(null)
        .build();
  }

  private ConnectionInfoDto buildConnectionInfoDtoException() {
    return ConnectionInfoDto.builder()
        .gmcReferenceNumber(gmcRef)
        .designatedBody(designatedBody1)
        .exceptionReason(INVALID_CREDENTIALS.getMessage())
        .build();
  }

  private ConnectionInfoDto buildConnectionInfoDtoExceptionResult() {
    return ConnectionInfoDto.builder()
        .tcsPersonId((long) 111)
        .gmcReferenceNumber(gmcRef)
        .doctorFirstName(firstName)
        .doctorLastName(lastName)
        .submissionDate(submissionDate)
        .programmeName(programmeName)
        .designatedBody(designatedBody)
        .programmeOwner(programmeOwner)
        .connectionStatus(status)
        .exceptionReason(INVALID_CREDENTIALS.getMessage())
        .build();
  }

}
