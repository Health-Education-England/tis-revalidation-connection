package uk.nhs.hee.tis.revalidation.connection.service;

import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.toList;
import static uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestType.ADD;
import static uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestType.HIDE;
import static uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestType.REMOVE;
import static uk.nhs.hee.tis.revalidation.connection.entity.GmcResponseCode.SUCCESS;
import static uk.nhs.hee.tis.revalidation.connection.entity.GmcResponseCode.fromCode;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.dto.UpdateConnectionDto;
import uk.nhs.hee.tis.revalidation.connection.dto.UpdateConnectionResponseDto;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionDto;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionHistoryDto;
import uk.nhs.hee.tis.revalidation.connection.dto.DoctorInfoDto;
import uk.nhs.hee.tis.revalidation.connection.dto.GmcConnectionResponseDto;
import uk.nhs.hee.tis.revalidation.connection.entity.AddConnectionReasonCode;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestLog;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestType;
import uk.nhs.hee.tis.revalidation.connection.entity.GmcResponseCode;
import uk.nhs.hee.tis.revalidation.connection.entity.HideConnectionLog;
import uk.nhs.hee.tis.revalidation.connection.entity.RemoveConnectionReasonCode;
import uk.nhs.hee.tis.revalidation.connection.message.ConnectionMessage;
import uk.nhs.hee.tis.revalidation.connection.repository.ConnectionRepository;
import uk.nhs.hee.tis.revalidation.connection.repository.HideConnectionRepository;

@Slf4j
@Service
public class ConnectionService {

  @Autowired
  private GmcClientService gmcClientService;

  @Autowired
  private ExceptionService exceptionService;

  @Autowired
  private ConnectionRepository repository;

  @Autowired
  private HideConnectionRepository hideRepository;

  @Autowired
  private RabbitTemplate rabbitTemplate;

  @Value("${app.rabbit.exchange}")
  private String exchange;

  @Value("${app.rabbit.connection.routingKey}")
  private String routingKey;

  public UpdateConnectionResponseDto addDoctor(final UpdateConnectionDto addDoctorDto) {
    return processConnectionRequest(addDoctorDto, ADD);
  }

  public UpdateConnectionResponseDto removeDoctor(final UpdateConnectionDto removeDoctorDto) {
    return processConnectionRequest(removeDoctorDto, REMOVE);
  }

  public UpdateConnectionResponseDto hideConnection(final UpdateConnectionDto hideConnectionDto) {
    return processHideConnection(hideConnectionDto, HIDE);
  }

  // get all connection history for a trainee
  public ConnectionDto getTraineeConnectionInfo(final String gmcId) {
    log.info("Fetching connections info for GmcId: {}", gmcId);
    final ConnectionDto connectionDto = new ConnectionDto();
    final var connections = repository.findAllByGmcIdOrderByRequestTimeDesc(gmcId);
    final var allConnectionsForTrainee = connections.stream().map(connection -> {
      String reasonMessage = "";
      if (connection.getRequestType().equals(ADD)) {
        reasonMessage = AddConnectionReasonCode.fromCode(connection.getReason());
      }
      else if (connection.getRequestType().equals(REMOVE)) {
        reasonMessage = RemoveConnectionReasonCode.fromCode(connection.getReason());
      }

      return ConnectionHistoryDto.builder()
          .connectionId(connection.getId())
          .gmcId(connection.getGmcId())
          .gmcClientId(connection.getGmcClientId())
          .newDesignatedBodyCode(connection.getNewDesignatedBodyCode())
          .previousDesignatedBodyCode(connection.getPreviousDesignatedBodyCode())
          .reason(connection.getReason())
          .reasonMessage(reasonMessage)
          .requestType(connection.getRequestType())
          .requestTime(connection.getRequestTime())
          .responseCode(connection.getResponseCode())
          .responseMessage(GmcResponseCode.fromCodeToMessage(connection.getResponseCode()))
          .build();
    }).collect(toList());
    connectionDto.setConnectionHistory(allConnectionsForTrainee);

    return connectionDto;
  }

  public List<String> getAllHiddenConnections() {
    final var allConnections = hideRepository.findAll();
    final var hiddenGmcIds = allConnections.stream().map(hidden -> {
      return hidden.getGmcId();
    }).collect(toList());
    return hiddenGmcIds;
  }

  private UpdateConnectionResponseDto processConnectionRequest(final UpdateConnectionDto addDoctorDto,
      final ConnectionRequestType connectionRequestType) {

    final var changeReason = addDoctorDto.getChangeReason();
    final var designatedBodyCode = addDoctorDto.getDesignatedBodyCode();
    final var addRemoveResponse = addDoctorDto.getDoctors().stream().map(doctor -> {
      final var gmcResponse = delegateRequest(changeReason, designatedBodyCode, doctor,
          connectionRequestType);
      return handleGmcResponse(doctor.getGmcId(), changeReason, designatedBodyCode,
          doctor.getCurrentDesignatedBodyCode(), gmcResponse, connectionRequestType);
    }).filter(response -> !response.getMessage().equals(SUCCESS.getMessage()))
        .findAny();
    if (addRemoveResponse.isPresent()) {
      final var errorMessage = getReturnMessage(addRemoveResponse.get().getMessage(), addDoctorDto.getDoctors().size());
      return UpdateConnectionResponseDto.builder().message(errorMessage).build();
    }
    return UpdateConnectionResponseDto.builder().message(SUCCESS.getMessage()).build();
  }

  //delegate request to GMC Client
  private GmcConnectionResponseDto delegateRequest(final String changeReason, final String designatedBodyCode,
      final DoctorInfoDto doctor, final ConnectionRequestType connectionRequestType) {
    GmcConnectionResponseDto gmcResponse = null;
    if (ADD == connectionRequestType) {
      gmcResponse = gmcClientService.tryAddDoctor(doctor.getGmcId(), changeReason, designatedBodyCode);
    } else {
      gmcResponse = gmcClientService.tryRemoveDoctor(doctor.getGmcId(), changeReason, doctor.getCurrentDesignatedBodyCode());
    }
    return gmcResponse;
  }

  // Handle Gmc response and take appropriate actions
  private UpdateConnectionResponseDto handleGmcResponse(final String gmcId, final String changeReason,
      final String designatedBodyCode, final String currentDesignatedBodyCode,
      final GmcConnectionResponseDto gmcResponse, final ConnectionRequestType connectionRequestType) {

    final var connectionRequestLog = ConnectionRequestLog.builder()
        .id(UUID.randomUUID().toString())
        .gmcId(gmcId)
        .gmcClientId(gmcResponse.getGmcRequestId())
        .newDesignatedBodyCode(designatedBodyCode)
        .previousDesignatedBodyCode(currentDesignatedBodyCode)
        .reason(changeReason)
        .requestType(connectionRequestType)
        .responseCode(gmcResponse.getReturnCode())
        .requestTime(now())
        .build();

    repository.save(connectionRequestLog);

    sendToRabbitOrExceptionLogs(gmcId, designatedBodyCode, gmcResponse.getReturnCode());
    final var gmcResponseCode = fromCode(gmcResponse.getReturnCode());
    final var responseMessage = gmcResponseCode != null ? gmcResponseCode.getMessage() : "";
    return UpdateConnectionResponseDto.builder().message(responseMessage).build();
  }

  //If success put message into queue to update doctors for DB otherwise log message into exception logs.
  private void sendToRabbitOrExceptionLogs(final String gmcId, final String designatedBodyCode,
      final String returnCode) {
    if (SUCCESS.getCode().equals(returnCode)) {
      final var connectionMessage = ConnectionMessage.builder()
          .gmcId(gmcId)
          .designatedBodyCode(designatedBodyCode)
          .build();
      log.info("Sending message to rabbit to remove designated body code");
      rabbitTemplate.convertAndSend(exchange, routingKey, connectionMessage);
    } else {
      exceptionService.createExceptionLog(gmcId, returnCode);
    }
  }

  //if bulk request then return generic failure message else gmc error message
  private String getReturnMessage(final String message, final int requestSize) {
    if( requestSize > 1) {
      return "Some changes have failed with GMC, please check the exceptions queue";
    }
    return message;
  }

  private UpdateConnectionResponseDto processHideConnection(final UpdateConnectionDto hideConnectionDto,
      final ConnectionRequestType connectionRequestType) {
    final var changeReason = hideConnectionDto.getChangeReason();
    hideConnectionDto.getDoctors().forEach(doctor -> saveToDatabase(doctor.getGmcId(), changeReason, connectionRequestType));
    return UpdateConnectionResponseDto.builder().message("Record has been hidden").build();
  }

  private void saveToDatabase(final String gmcId, final String changeReason, final ConnectionRequestType connectionRequestType) {
    final var hideConnectionLog = HideConnectionLog.builder()
        .id(UUID.randomUUID().toString())
        .gmcId(gmcId)
        .reason(changeReason)
        .requestType(connectionRequestType)
        .requestTime(now())
        .build();
    hideRepository.save(hideConnectionLog);
  }

}
