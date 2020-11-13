package uk.nhs.hee.tis.revalidation.connection.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.connection.dto.AddRemoveDoctorDto;
import uk.nhs.hee.tis.revalidation.connection.dto.GmcConnectionResponseDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestLog;
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
  private GmcConnectionResponseDto gmcConnectionResponseDto;

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
  }

  @Test
  public void shouldAddADoctor() {
    final var addDoctorDto = AddRemoveDoctorDto.builder()
        .gmcId(gmcId)
        .changeReason(changeReason)
        .designatedBodyCode(designatedBodyCode)
        .build();

    when(gmcClientService.addDoctor(addDoctorDto)).thenReturn(gmcConnectionResponseDto);
    when(gmcConnectionResponseDto.getGmcRequestId()).thenReturn(gmcRequestId);
    when(gmcConnectionResponseDto.getReturnCode()).thenReturn(returnCode);
    connectionService.addDoctor(addDoctorDto);
    verify(repository).save(any(ConnectionRequestLog.class));
  }

  @Test
  public void shouldRemoveADoctor() {
    final var removeDoctorDto = AddRemoveDoctorDto.builder()
        .gmcId(gmcId)
        .changeReason(changeReason)
        .designatedBodyCode(designatedBodyCode)
        .build();

    when(gmcClientService.removeDoctor(removeDoctorDto)).thenReturn(gmcConnectionResponseDto);
    when(gmcConnectionResponseDto.getGmcRequestId()).thenReturn(gmcRequestId);
    when(gmcConnectionResponseDto.getReturnCode()).thenReturn(returnCode);
    connectionService.removeDoctor(removeDoctorDto);
    verify(repository).save(any(ConnectionRequestLog.class));
  }

}
