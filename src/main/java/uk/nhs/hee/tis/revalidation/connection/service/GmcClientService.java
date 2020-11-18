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
import uk.nhs.hee.tis.revalidation.connection.dto.AddRemoveDoctorDto;
import uk.nhs.hee.tis.revalidation.connection.dto.GmcConnectionResponseDto;

@Slf4j
@Service
public class GmcClientService {

  private static final String TRY_ADD_DOCTOR = "TryAddDoctor";
  private static final String TRY_REMOVE_DOCTOR = "TryRemoveDoctor";

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

  public GmcConnectionResponseDto addDoctor(final AddRemoveDoctorDto addDoctorDto) {
    log.info("Preparing to submit add doctor request to GMC: {}", addDoctorDto);
    final var tryAddDoctor = prepareAddDoctorRequest(addDoctorDto);

    final var tryAddDoctorResponse = submitTryAddDoctor(tryAddDoctor);
    final var tryAddDoctorResult = tryAddDoctorResponse.getTryAddDoctorResult();
    return GmcConnectionResponseDto.builder()
        .clientRequestId(tryAddDoctorResult.getClientRequestID())
        .gmcRequestId(tryAddDoctorResult.getGMCRequestID())
        .returnCode(tryAddDoctorResult.getReturnCode())
        .build();
  }

  public GmcConnectionResponseDto removeDoctor(final AddRemoveDoctorDto removeDoctorDto) {
    log.info("Preparing to submit remove doctor request to GMC: {}", removeDoctorDto);
    final var tryRemoveDoctor = prepareRemoveDoctorRequest(removeDoctorDto);

    final var tryRemoveDoctorResponse = submitTryRemoveDoctor(tryRemoveDoctor);
    final var tryRemoveDoctorResult = tryRemoveDoctorResponse.getTryRemoveDoctorResult();
    return GmcConnectionResponseDto.builder()
        .clientRequestId(tryRemoveDoctorResult.getClientRequestID())
        .gmcRequestId(tryRemoveDoctorResult.getGMCRequestID())
        .returnCode(tryRemoveDoctorResult.getReturnCode())
        .build();
  }

  private TryAddDoctor prepareAddDoctorRequest(final AddRemoveDoctorDto addDoctorDto) {
    final var tryAddDoctor = new TryAddDoctor();
    final var tryAddDoctorRequest = new TryAddDoctorRequest();
    tryAddDoctorRequest.setDoctorUID(addDoctorDto.getGmcId());
    tryAddDoctorRequest.setChangeReason(addDoctorDto.getChangeReason());
    tryAddDoctorRequest.setDesignatedBodyCode(addDoctorDto.getDesignatedBodyCode());
    tryAddDoctorRequest.setClientRequestID(addDoctorDto.getGmcId());
    tryAddDoctor.setRecReq(tryAddDoctorRequest);
    tryAddDoctor.setUsername(gmcUserName);
    tryAddDoctor.setPassword(gmcPassword);
    return tryAddDoctor;
  }

  private TryRemoveDoctor prepareRemoveDoctorRequest(final AddRemoveDoctorDto removeDoctorDto) {
    final var tryRemoveDoctor = new TryRemoveDoctor();
    final var tryRemoveDoctorRequest = new TryRemoveDoctorRequest();
    tryRemoveDoctorRequest.setDoctorUID(removeDoctorDto.getGmcId());
    tryRemoveDoctorRequest.setChangeReason(removeDoctorDto.getChangeReason());
    tryRemoveDoctorRequest.setClientRequestID(removeDoctorDto.getGmcId());
    tryRemoveDoctorRequest.setDesignatedBodyCode(removeDoctorDto.getDesignatedBodyCode());
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
