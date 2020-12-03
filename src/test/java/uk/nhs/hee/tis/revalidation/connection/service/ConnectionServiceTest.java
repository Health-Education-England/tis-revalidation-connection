package uk.nhs.hee.tis.revalidation.connection.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.github.javafaker.Faker;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.nhs.hee.tis.revalidation.connection.dto.AddRemoveDoctorDto;
import uk.nhs.hee.tis.revalidation.connection.dto.DoctorInfoDto;
import uk.nhs.hee.tis.revalidation.connection.dto.GmcConnectionResponseDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestLog;
import uk.nhs.hee.tis.revalidation.connection.message.ConnectionMessage;
import uk.nhs.hee.tis.revalidation.connection.repository.ConnectionRepository;

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
  private RabbitTemplate rabbitTemplate;

  @Mock
  private GmcConnectionResponseDto gmcConnectionResponseDto;

  @Mock
  private ExceptionService exceptionService;

  private String changeReason;
  private String designatedBodyCode;
  private String gmcId;
  private String gmcRequestId;
  private String returnCode;

  @BeforeEach
  public void setup() {
    changeReason = faker.lorem().sentence();
    designatedBodyCode = faker.number().digits(8);
    gmcId = faker.number().digits(8);
    gmcRequestId = faker.random().toString();
    returnCode = "0";

    setField(connectionService, "exchange", "exchange");
    setField(connectionService, "routingKey", "routingKey");
  }

  @Test
  public void shouldAddADoctor() {
    final var addDoctorDto = AddRemoveDoctorDto.builder()
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
  public void shouldRemoveADoctor() {
    final var removeDoctorDto = AddRemoveDoctorDto.builder()
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
  public void shouldAddToExceptionWhenRemoveADoctorFails() {
    returnCode = "90";
    final var removeDoctorDto = AddRemoveDoctorDto.builder()
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
    verify(exceptionService).createExceptionLog(gmcId, returnCode);
  }

  private List<DoctorInfoDto> buildDoctorsList() {
    final var doc1 = DoctorInfoDto.builder().gmcId(gmcId)
        .currentDesignatedBodyCode(designatedBodyCode).build();
    final var doc2 = DoctorInfoDto.builder().gmcId(gmcId)
        .currentDesignatedBodyCode(designatedBodyCode).build();
    return List.of(doc1, doc2);
  }
}
