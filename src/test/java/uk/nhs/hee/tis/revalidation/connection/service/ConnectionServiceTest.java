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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.nhs.hee.tis.revalidation.connection.dto.DoctorInfoDto;
import uk.nhs.hee.tis.revalidation.connection.dto.GmcConnectionResponseDto;
import uk.nhs.hee.tis.revalidation.connection.dto.UpdateConnectionDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestLog;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestType;
import uk.nhs.hee.tis.revalidation.connection.entity.GmcResponseCode;
import uk.nhs.hee.tis.revalidation.connection.entity.HideConnectionLog;
import uk.nhs.hee.tis.revalidation.connection.message.ConnectionMessage;
import uk.nhs.hee.tis.revalidation.connection.repository.ConnectionRepository;
import uk.nhs.hee.tis.revalidation.connection.repository.HideConnectionRepository;

@ExtendWith(MockitoExtension.class)
class ConnectionServiceTest {

  private final Faker faker = new Faker();

  @InjectMocks
  private ConnectionService connectionService;

  @Mock
  private GmcClientService gmcClientService;

  @Mock
  private ConnectionRepository repository;

  @Mock
  private HideConnectionRepository hideRepository;

  @Mock
  private RabbitTemplate rabbitTemplate;

  @Mock
  private GmcConnectionResponseDto gmcConnectionResponseDto;

  @Mock
  private ExceptionService exceptionService;

  private String changeReason;
  private String designatedBodyCode;
  private String programmeOwnerDesignatedBodyCode;
  private String gmcId;
  private String gmcRequestId;
  private String returnCode;
  private LocalDate submissionDate;

  private String connectionId;
  private String gmcClientId;
  private String newDesignatedBodyCode;
  private String previousDesignatedBodyCode;
  private String reasonAdd;
  private String reasonMessageAdd;
  private ConnectionRequestType requestTypeAdd;
  private String reasonRemove;
  private String reasonMessageRemove;
  private ConnectionRequestType requestTypeRemove;
  private LocalDateTime requestTime;
  private String responseCode;
  private String reasonHide;
  private ConnectionRequestType requestTypeHide;

  @BeforeEach
  public void setup() {
    changeReason = faker.lorem().sentence();
    designatedBodyCode = faker.number().digits(8);
    gmcId = faker.number().digits(8);
    gmcRequestId = faker.random().toString();
    returnCode = "0";
    submissionDate = LocalDate.now();

    connectionId = faker.number().digits(20);
    gmcClientId = faker.number().digits(8);
    newDesignatedBodyCode = faker.number().digits(8);
    previousDesignatedBodyCode = faker.number().digits(8);
    requestTypeAdd = ConnectionRequestType.ADD;
    reasonAdd = "2";
    reasonMessageAdd = "Conflict of Interest";
    requestTypeRemove = ConnectionRequestType.REMOVE;
    reasonRemove = "2";
    reasonMessageRemove = "Doctor has retired";
    requestTime = LocalDateTime.now().minusDays(-1);
    responseCode = faker.number().digits(5);
    reasonHide = "2";
    requestTypeHide = ConnectionRequestType.HIDE;

    programmeOwnerDesignatedBodyCode = faker.number().digits(8);

    setField(connectionService, "esExchange", "esExchange");
    setField(connectionService, "routingKey", "routingKey");
  }

  @Test
  void shouldAddADoctor() {
    final var addDoctorDto = UpdateConnectionDto.builder()
        .changeReason(changeReason)
        .designatedBodyCode(designatedBodyCode)
        .doctors(buildDoctorsList())
        .build();

    when(gmcClientService.tryAddDoctor(gmcId, changeReason, designatedBodyCode))
        .thenReturn(gmcConnectionResponseDto);
    when(gmcConnectionResponseDto.getGmcRequestId()).thenReturn(gmcRequestId);
    when(gmcConnectionResponseDto.getReturnCode()).thenReturn(returnCode);
    when(gmcConnectionResponseDto.getSubmissionDate()).thenReturn(submissionDate);
    connectionService.addDoctor(addDoctorDto);
    var message = ConnectionMessage.builder()
        .gmcId(gmcId)
        .designatedBodyCode(designatedBodyCode)
        .submissionDate(submissionDate)
        .build();
    verify(rabbitTemplate, times(2)).convertAndSend("esExchange", "routingKey", message);
    verify(repository, times(2)).save(any(ConnectionRequestLog.class));
  }

  @Test
  void shouldRemoveADoctor() {
    final var removeDoctorDto = UpdateConnectionDto.builder()
        .changeReason(changeReason)
        .designatedBodyCode(designatedBodyCode)
        .doctors(buildDoctorsList())
        .build();

    when(gmcClientService.tryRemoveDoctor(gmcId, changeReason, designatedBodyCode))
        .thenReturn(gmcConnectionResponseDto);
    when(gmcConnectionResponseDto.getGmcRequestId()).thenReturn(gmcRequestId);
    when(gmcConnectionResponseDto.getReturnCode()).thenReturn(returnCode);
    connectionService.removeDoctor(removeDoctorDto);
    var message = ConnectionMessage.builder().gmcId(gmcId).designatedBodyCode(designatedBodyCode)
        .build();
    verify(rabbitTemplate, times(2)).convertAndSend("esExchange", "routingKey", message);
    verify(repository, times(2)).save(any(ConnectionRequestLog.class));
  }

  @Test
  void shouldHideADoctorConnection() {
    final var hideDoctorDto = UpdateConnectionDto.builder()
        .changeReason(changeReason)
        .doctors(buildDoctorsList())
        .build();
    connectionService.hideConnection(hideDoctorDto);
    verify(hideRepository, times(2)).save(any(HideConnectionLog.class));
  }

  @Test
  void shouldUnhideADoctorConnection() {
    final var unhideDoctorDto = UpdateConnectionDto.builder()
        .changeReason(changeReason)
        .doctors(buildDoctorsList())
        .build();
    connectionService.unhideConnection(unhideDoctorDto);
    verify(hideRepository, times(2)).deleteById(any(String.class));
  }

  @Test
  void shouldAddToExceptionWhenRemoveADoctorFails() {
    returnCode = "90";
    final var removeDoctorDto = UpdateConnectionDto.builder()
        .changeReason(changeReason)
        .designatedBodyCode(designatedBodyCode)
        .doctors(buildDoctorsList())
        .build();
    final String exceptionMessage = GmcResponseCode.fromCode(returnCode).getMessage();

    when(gmcClientService.tryRemoveDoctor(gmcId, changeReason, designatedBodyCode))
        .thenReturn(gmcConnectionResponseDto);
    when(gmcConnectionResponseDto.getGmcRequestId()).thenReturn(gmcRequestId);
    when(gmcConnectionResponseDto.getReturnCode()).thenReturn(returnCode);
    when(gmcConnectionResponseDto.getSubmissionDate()).thenReturn(submissionDate);
    connectionService.removeDoctor(removeDoctorDto);
    var message = ConnectionMessage.builder()
        .gmcId(gmcId)
        .designatedBodyCode(designatedBodyCode)
        .submissionDate(submissionDate)
        .build();
    verify(exceptionService, times(2)).createExceptionLog(gmcId, exceptionMessage);
  }

  @Test
  void shouldReturnAllConnectionsForADoctor() throws Exception {
    final var connection1 = prepareConnectionAdd();
    final var connection2 = prepareConnectionRemove();
    when(repository.findAllByGmcIdOrderByRequestTimeDesc(gmcId))
        .thenReturn(List.of(connection1, connection2));
    var connectionDto = connectionService.getTraineeConnectionInfo(gmcId);
    var connections = connectionDto.getConnectionHistory();
    assertThat(connections.size(), is(2));
    final var connectionDto1 = connections.get(0);
    assertThat(connectionDto1.getConnectionId(), is(connectionId));
    assertThat(connectionDto1.getGmcId(), is(gmcId));
    assertThat(connectionDto1.getNewDesignatedBodyCode(), is(newDesignatedBodyCode));
    assertThat(connectionDto1.getPreviousDesignatedBodyCode(), is(previousDesignatedBodyCode));
    assertThat(connectionDto1.getReason(), is(reasonAdd));
    assertThat(connectionDto1.getReasonMessage(), is(reasonMessageAdd));
    assertThat(connectionDto1.getRequestTime(), is(requestTime));
    final var connectionDto2 = connections.get(1);
    assertThat(connectionDto2.getReason(), is(reasonRemove));
    assertThat(connectionDto2.getReasonMessage(), is(reasonMessageRemove));
  }

  @Test
  void shouldNotFailWhenThereIsNoConnectionForADoctorInTheService() throws Exception {
    when(repository.findAllByGmcIdOrderByRequestTimeDesc(gmcId)).thenReturn(List.of());
    var connectionDto = connectionService.getTraineeConnectionInfo(gmcId);
    var connections = connectionDto.getConnectionHistory();
    assertThat(connections.size(), is(0));
  }

  @Test
  void shouldReturnAllHiddenConnections() throws Exception {
    final var hiddenConnection = prepareHiddenConnection();
    when(hideRepository.findAll()).thenReturn(List.of(hiddenConnection));
    var hiddenGmcIds = connectionService.getAllHiddenConnections();
    assertThat(hiddenGmcIds.size(), is(1));
    final var hiddenGmcId = hiddenGmcIds.get(0);
    assertThat(hiddenGmcId, is(gmcId));
  }

  @Test
  void shouldNotFailWhenThereIsNoHiddenConnection() throws Exception {
    when(hideRepository.findAll()).thenReturn(List.of());
    var hiddenGmcIds = connectionService.getAllHiddenConnections();
    assertThat(hiddenGmcIds.size(), is(0));
  }

  private ConnectionRequestLog prepareConnectionAdd() {
    return ConnectionRequestLog.builder()
        .id(connectionId)
        .gmcId(gmcId)
        .gmcClientId(gmcClientId)
        .newDesignatedBodyCode(newDesignatedBodyCode)
        .previousDesignatedBodyCode(previousDesignatedBodyCode)
        .reason(reasonAdd)
        .requestType(requestTypeAdd)
        .requestTime(requestTime)
        .responseCode(responseCode)
        .build();
  }

  private ConnectionRequestLog prepareConnectionRemove() {
    return ConnectionRequestLog.builder()
        .id(connectionId)
        .gmcId(gmcId)
        .gmcClientId(gmcClientId)
        .newDesignatedBodyCode(newDesignatedBodyCode)
        .previousDesignatedBodyCode(previousDesignatedBodyCode)
        .reason(reasonRemove)
        .requestType(requestTypeRemove)
        .requestTime(requestTime)
        .responseCode(responseCode)
        .build();
  }

  private List<DoctorInfoDto> buildDoctorsList() {
    final var doc1 = DoctorInfoDto.builder().gmcId(gmcId)
        .currentDesignatedBodyCode(designatedBodyCode)
        .programmeOwnerDesignatedBodyCode(designatedBodyCode)
        .build();
    final var doc2 = DoctorInfoDto.builder().gmcId(gmcId)
        .currentDesignatedBodyCode(designatedBodyCode)
        .programmeOwnerDesignatedBodyCode(designatedBodyCode)
        .build();
    return List.of(doc1, doc2);
  }

  private HideConnectionLog prepareHiddenConnection() {
    return HideConnectionLog.builder()
        .gmcId(gmcId)
        .reason(reasonHide)
        .requestType(requestTypeHide)
        .requestTime(requestTime)
        .build();
  }
}
