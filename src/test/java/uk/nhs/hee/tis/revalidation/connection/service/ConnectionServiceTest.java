package uk.nhs.hee.tis.revalidation.connection.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.github.javafaker.Faker;
import java.time.LocalDateTime;
import java.util.Arrays;
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

    setField(connectionService, "exchange", "exchange");
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
    connectionService.addDoctor(addDoctorDto);
    var message = ConnectionMessage.builder().gmcId(gmcId).designatedBodyCode(designatedBodyCode)
        .build();
    verify(rabbitTemplate, times(2)).convertAndSend("exchange", "routingKey", message);
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
    verify(rabbitTemplate, times(2)).convertAndSend("exchange", "routingKey", message);
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

    when(gmcClientService.tryRemoveDoctor(gmcId, changeReason, designatedBodyCode))
        .thenReturn(gmcConnectionResponseDto);
    when(gmcConnectionResponseDto.getGmcRequestId()).thenReturn(gmcRequestId);
    when(gmcConnectionResponseDto.getReturnCode()).thenReturn(returnCode);
    connectionService.removeDoctor(removeDoctorDto);
    var message = ConnectionMessage.builder().gmcId(gmcId).designatedBodyCode(designatedBodyCode)
        .build();
    verify(exceptionService, times(2)).createExceptionLog(gmcId, returnCode);
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

  @Test
  void shouldAddToExceptionQueueIfTraineeCurrentDbcAndProgrammeOwnerDbcDoesNotMatch()
      throws Exception {
    final var updateConnectionDto = UpdateConnectionDto.builder()
        .changeReason(changeReason)
        .designatedBodyCode(designatedBodyCode)
        .doctors(Arrays.asList(DoctorInfoDto.builder().gmcId(gmcId)
            .currentDesignatedBodyCode(designatedBodyCode)
            .programmeOwnerDesignatedBodyCode(programmeOwnerDesignatedBodyCode)
            .build()))
        .build();
    connectionService.addDoctor(updateConnectionDto);
    String errorMessage = "Doctor's current designated body "
        + "does not match with current programme owner";
    verify(exceptionService).sendToExceptionQueue(gmcId, errorMessage);
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
