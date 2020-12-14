package uk.nhs.hee.tis.revalidation.connection.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import uk.nhs.hee.tis.gmc.client.generated.TryAddDoctor;
import uk.nhs.hee.tis.gmc.client.generated.TryAddDoctorRequest;
import uk.nhs.hee.tis.gmc.client.generated.TryAddDoctorResponse;
import uk.nhs.hee.tis.gmc.client.generated.TryRemoveDoctor;
import uk.nhs.hee.tis.gmc.client.generated.TryRemoveDoctorRequest;
import uk.nhs.hee.tis.gmc.client.generated.TryRemoveDoctorResponse;
import uk.nhs.hee.tis.revalidation.connection.dto.GmcConnectionResponseDto;

@Slf4j
@Service
public class GmcClientService {

  private static final String TRY_ADD_DOCTOR = "TryAddDoctor";
  private static final String TRY_REMOVE_DOCTOR = "TryRemoveDoctor";
  private static final String INTERNAL_USER = "Admin"; //TODO: update when pass real user

  @Autowired
  private WebServiceTemplate webServiceTemplate;

  @Value("${app.gmc.url}")
  private String gmcConnectUrl;

  @Value("${app.gmc.gmcUsername}")
  private String gmcUserName;

  @Value("${app.gmc.gmcPassword}")
  private String gmcPassword;

  @Value("${app.gmc.soapActionBase}")
  private String gmcSoapBaseAction;

  public GmcConnectionResponseDto tryAddDoctor(final String gmcId, final String changeReason,
      final String designatedBodyCode) {
    log.info("Preparing to submit add doctor request to GMC: {}", gmcId);
    final var tryAddDoctor = prepareAddDoctorRequest(gmcId, changeReason, designatedBodyCode);

    final var tryAddDoctorResponse = submitTryAddDoctor(tryAddDoctor);
    final var tryAddDoctorResult = tryAddDoctorResponse.getTryAddDoctorResult();
    return GmcConnectionResponseDto.builder()
        .clientRequestId(tryAddDoctorResult.getClientRequestID())
        .gmcRequestId(tryAddDoctorResult.getGMCRequestID())
        .returnCode(tryAddDoctorResult.getReturnCode())
        .build();
  }

  public GmcConnectionResponseDto tryRemoveDoctor(final String gmcId, final String changeReason,
      final String designatedBodyCode) {
    log.info("Preparing to submit remove doctor request to GMC: {}", gmcId);
    final var tryRemoveDoctor = prepareRemoveDoctorRequest(gmcId, changeReason, designatedBodyCode);

    final var tryRemoveDoctorResponse = submitTryRemoveDoctor(tryRemoveDoctor);
    final var tryRemoveDoctorResult = tryRemoveDoctorResponse.getTryRemoveDoctorResult();
    return GmcConnectionResponseDto.builder()
        .clientRequestId(tryRemoveDoctorResult.getClientRequestID())
        .gmcRequestId(tryRemoveDoctorResult.getGMCRequestID())
        .returnCode(tryRemoveDoctorResult.getReturnCode())
        .build();
  }

  private TryAddDoctor prepareAddDoctorRequest(final String gmcId, final String changeReason,
      final String designatedBodyCode) {
    final var tryAddDoctor = new TryAddDoctor();
    final var tryAddDoctorRequest = new TryAddDoctorRequest();
    tryAddDoctorRequest.setDoctorUID(gmcId);
    tryAddDoctorRequest.setChangeReason(changeReason);
    tryAddDoctorRequest.setDesignatedBodyCode(designatedBodyCode);
    tryAddDoctorRequest.setClientRequestID(gmcId);
    tryAddDoctorRequest.setInternalUser(INTERNAL_USER);
    tryAddDoctor.setRecReq(tryAddDoctorRequest);
    tryAddDoctor.setUsername(gmcUserName);
    tryAddDoctor.setPassword(gmcPassword);
    return tryAddDoctor;
  }

  private TryRemoveDoctor prepareRemoveDoctorRequest(final String gmcId, final String changeReason,
      final String designatedBodyCode) {
    final var tryRemoveDoctor = new TryRemoveDoctor();
    final var tryRemoveDoctorRequest = new TryRemoveDoctorRequest();
    tryRemoveDoctorRequest.setDoctorUID(gmcId);
    tryRemoveDoctorRequest.setChangeReason(changeReason);
    tryRemoveDoctorRequest.setClientRequestID(gmcId);
    tryRemoveDoctorRequest.setDesignatedBodyCode(designatedBodyCode);
    tryRemoveDoctorRequest.setInternalUser(INTERNAL_USER);
    tryRemoveDoctor.setRecReq(tryRemoveDoctorRequest);
    tryRemoveDoctor.setUsername(gmcUserName);
    tryRemoveDoctor.setPassword(gmcPassword);
    return tryRemoveDoctor;
  }

  private TryAddDoctorResponse submitTryAddDoctor(final TryAddDoctor tryAddDoctor) {
    return (TryAddDoctorResponse) webServiceTemplate.marshalSendAndReceive(gmcConnectUrl,
        tryAddDoctor, new SoapActionCallback(gmcSoapBaseAction + TRY_ADD_DOCTOR));
  }

  private TryRemoveDoctorResponse submitTryRemoveDoctor(final TryRemoveDoctor tryRemoveDoctor) {
    return (TryRemoveDoctorResponse) webServiceTemplate.marshalSendAndReceive(gmcConnectUrl,
        tryRemoveDoctor, new SoapActionCallback(gmcSoapBaseAction + TRY_REMOVE_DOCTOR));
  }

}
