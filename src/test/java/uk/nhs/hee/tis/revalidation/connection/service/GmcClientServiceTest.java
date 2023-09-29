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
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import java.time.LocalDate;
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
import uk.nhs.hee.tis.revalidation.connection.dto.DoctorInfoDto;
import uk.nhs.hee.tis.revalidation.connection.dto.UpdateConnectionDto;

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
  private String submissionDateString;
  private LocalDate submissionDate;
  private String returnCode;

  @BeforeEach
  public void setup() {
    changeReason = faker.lorem().sentence();
    designatedBodyCode = faker.number().digits(8);
    gmcId = faker.number().digits(8);
    gmcRequestId = faker.random().toString();
    returnCode = "0";
    submissionDate = LocalDate.now();
    /* From GMC Docs - Submission Date "Should be a valid date in the
    format DD/MM/YYYY" */
    submissionDateString = String.format("%02d/%02d/%04d", submissionDate.getDayOfMonth(),
        submissionDate.getMonthValue(), submissionDate.getYear());

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
  void shouldSubmitTryAddDoctorRequest() {

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
    when(tryAddDoctorResponseCT.getSubmissionDate()).thenReturn(submissionDateString);
    when(tryAddDoctorResponseCT.getReturnCode()).thenReturn(returnCode);
    final var gmcConnectionResponseDto =
        gmcClientService.tryAddDoctor(gmcId, changeReason, designatedBodyCode);

    assertThat(gmcConnectionResponseDto.getClientRequestId(), is(gmcId));
    assertThat(gmcConnectionResponseDto.getGmcRequestId(), is(gmcRequestId));
    assertThat(gmcConnectionResponseDto.getSubmissionDate(), is(submissionDate));
    assertThat(gmcConnectionResponseDto.getReturnCode(), is(returnCode));
  }

  @Test
  void shouldSubmitTryRemoveDoctorRequest() {

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
