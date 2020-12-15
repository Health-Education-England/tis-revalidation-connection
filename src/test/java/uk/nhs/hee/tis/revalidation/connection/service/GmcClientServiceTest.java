package uk.nhs.hee.tis.revalidation.connection.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import uk.nhs.hee.tis.gmc.client.generated.TryAddDoctor;
import uk.nhs.hee.tis.gmc.client.generated.TryAddDoctorResponse;
import uk.nhs.hee.tis.gmc.client.generated.TryAddDoctorResponseCT;
import uk.nhs.hee.tis.gmc.client.generated.TryRemoveDoctor;
import uk.nhs.hee.tis.gmc.client.generated.TryRemoveDoctorResponse;
import uk.nhs.hee.tis.gmc.client.generated.TryRemoveDoctorResponseCT;
import uk.nhs.hee.tis.revalidation.connection.dto.UpdateConnectionDto;
import uk.nhs.hee.tis.revalidation.connection.dto.DoctorInfoDto;

@ExtendWith(MockitoExtension.class)
class GmcClientServiceTest {

  private final Faker faker = new Faker();

  @Mock
  private WebServiceTemplate webServiceTemplate;

  @Mock
  private TryAddDoctorResponse tryAddDoctorResponse;

  @Mock
  private TryAddDoctorResponseCT tryAddDoctorResponseCT;

  @Mock
  private TryRemoveDoctorResponse tryRemoveDoctorResponse;

  @Mock
  private TryRemoveDoctorResponseCT tryRemoveDoctorResponseCT;

  @InjectMocks
  private GmcClientService gmcClientService;

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

    final var gmcUrl = faker.internet().url();
    final var soapAction = faker.internet().url();
    final var userName = faker.name().username();
    final var password = faker.random().hex();
    ReflectionTestUtils.setField(gmcClientService, "gmcConnectUrl", gmcUrl);
    ReflectionTestUtils.setField(gmcClientService, "gmcUserName", userName);
    ReflectionTestUtils.setField(gmcClientService, "gmcPassword", password);
    ReflectionTestUtils.setField(gmcClientService, "gmcSoapBaseAction", soapAction);
  }

  @Test
  public void shouldSubmitTryAddDoctorRequest() {

    final var addDoctorDto = UpdateConnectionDto.builder()
        .changeReason(changeReason)
        .designatedBodyCode(designatedBodyCode)
        .doctors(buildDoctorsList())
        .build();

    when(webServiceTemplate.marshalSendAndReceive(any(String.class), any(
        TryAddDoctor.class), any(SoapActionCallback.class))).thenReturn(tryAddDoctorResponse);
    when(tryAddDoctorResponse.getTryAddDoctorResult()).thenReturn(tryAddDoctorResponseCT);
    when(tryAddDoctorResponseCT.getClientRequestID()).thenReturn(gmcId);
    when(tryAddDoctorResponseCT.getGMCRequestID()).thenReturn(gmcRequestId);
    when(tryAddDoctorResponseCT.getReturnCode()).thenReturn(returnCode);
    final var gmcConnectionResponseDto =
        gmcClientService.tryAddDoctor(gmcId, changeReason, designatedBodyCode);

    assertThat(gmcConnectionResponseDto.getClientRequestId(), is(gmcId));
    assertThat(gmcConnectionResponseDto.getGmcRequestId(), is(gmcRequestId));
    assertThat(gmcConnectionResponseDto.getReturnCode(), is(returnCode));
  }

  @Test
  public void shouldSubmitTryRemoveDoctorRequest() {

    final var removeDoctorDto = UpdateConnectionDto.builder()
        .changeReason(changeReason)
        .designatedBodyCode(designatedBodyCode)
        .doctors(buildDoctorsList())
        .build();

    when(webServiceTemplate.marshalSendAndReceive(any(String.class), any(
        TryRemoveDoctor.class), any(SoapActionCallback.class))).thenReturn(tryRemoveDoctorResponse);
    when(tryRemoveDoctorResponse.getTryRemoveDoctorResult()).thenReturn(tryRemoveDoctorResponseCT);
    when(tryRemoveDoctorResponseCT.getClientRequestID()).thenReturn(gmcId);
    when(tryRemoveDoctorResponseCT.getGMCRequestID()).thenReturn(gmcRequestId);
    when(tryRemoveDoctorResponseCT.getReturnCode()).thenReturn(returnCode);
    final var gmcConnectionResponseDto =
        gmcClientService.tryRemoveDoctor(gmcId, changeReason, designatedBodyCode);

    assertThat(gmcConnectionResponseDto.getClientRequestId(), is(gmcId));
    assertThat(gmcConnectionResponseDto.getGmcRequestId(), is(gmcRequestId));
    assertThat(gmcConnectionResponseDto.getReturnCode(), is(returnCode));
  }

  private List<DoctorInfoDto> buildDoctorsList() {
    final var doc1 = DoctorInfoDto.builder().gmcId(gmcId)
        .currentDesignatedBodyCode(designatedBodyCode).build();
    final var doc2 = DoctorInfoDto.builder().gmcId(gmcId)
        .currentDesignatedBodyCode(designatedBodyCode).build();
    return List.of(doc1, doc2);
  }
}
