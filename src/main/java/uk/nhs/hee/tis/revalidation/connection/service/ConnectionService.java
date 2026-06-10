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

import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.toList;
import static uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestType.ADD;
import static uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestType.REMOVE;
import static uk.nhs.hee.tis.revalidation.connection.entity.GmcResponseCode.DOCTOR_ALREADY_ASSOCIATED;
import static uk.nhs.hee.tis.revalidation.connection.entity.GmcResponseCode.SUCCESS;
import static uk.nhs.hee.tis.revalidation.connection.entity.GmcResponseCode.fromCode;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionDto;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionHistoryDto;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionLogDto;
import uk.nhs.hee.tis.revalidation.connection.dto.DoctorInfoDto;
import uk.nhs.hee.tis.revalidation.connection.dto.GmcConnectionResponseDto;
import uk.nhs.hee.tis.revalidation.connection.dto.UpdateConnectionDto;
import uk.nhs.hee.tis.revalidation.connection.dto.UpdateConnectionResponseDto;
import uk.nhs.hee.tis.revalidation.connection.entity.AddConnectionReasonCode;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionLog;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestLog;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestType;
import uk.nhs.hee.tis.revalidation.connection.entity.GmcResponseCode;
import uk.nhs.hee.tis.revalidation.connection.entity.RemoveConnectionReasonCode;
import uk.nhs.hee.tis.revalidation.connection.event.ConnectionChangedApplicationEvent;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionLogMapper;
import uk.nhs.hee.tis.revalidation.connection.message.ConnectionMessage;
import uk.nhs.hee.tis.revalidation.connection.message.payloads.IndexSyncMessage;
import uk.nhs.hee.tis.revalidation.connection.repository.ConnectionLogCustomRepository;
import uk.nhs.hee.tis.revalidation.connection.repository.ConnectionRepository;

@Slf4j
@Service
public class ConnectionService {

  private final GmcClientService gmcClientService;

  private final ExceptionLogService exceptionService;

  private final ConnectionRepository repository;

  private final ConnectionLogCustomRepository connectionLogCustomRepository;

  private final RabbitTemplate rabbitTemplate;

  private final ConnectionLogMapper connectionLogMapper;

  private final ApplicationEventPublisher applicationEventPublisher;

  private final AddConnectionStrategy addConnectionStrategy;

  private final RemoveConnectionStrategy removeConnectionStrategy;

  @Value("${app.rabbit.reval.exchange}")
  private String exchange;

  @Value("${app.rabbit.reval.routingKey.connection.manualupdate}")
  private String routingKey;

  @Value("${app.rabbit.reval.routingKey.connectionlog.essyncdata}")
  private String esSyncDataRoutingKey;

  private static final String UPDATED_BY_GMC = "Updated by GMC";

  public ConnectionService(GmcClientService gmcClientService, ExceptionLogService exceptionService,
      ConnectionRepository repository, ConnectionLogCustomRepository connectionLogCustomRepository,
      RabbitTemplate rabbitTemplate,
      ConnectionLogMapper connectionLogMapper,
      ApplicationEventPublisher applicationEventPublisher,
      AddConnectionStrategy addConnectionStrategy,
      RemoveConnectionStrategy removeConnectionStrategy) {
    this.gmcClientService = gmcClientService;
    this.exceptionService = exceptionService;
    this.repository = repository;
    this.connectionLogCustomRepository = connectionLogCustomRepository;
    this.rabbitTemplate = rabbitTemplate;
    this.connectionLogMapper = connectionLogMapper;
    this.applicationEventPublisher = applicationEventPublisher;
    this.addConnectionStrategy = addConnectionStrategy;
    this.removeConnectionStrategy = removeConnectionStrategy;
  }

  /**
   * Add doctors to a designated body by processing each doctor connection request with GMC.
   *
   * @param addDoctorDto the connection update request containing doctors to add
   * @return response indicating success or failure
   */
  public UpdateConnectionResponseDto bulkAddConnection(final UpdateConnectionDto addDoctorDto) {
    return processBulkConnectionRequest(addDoctorDto, addConnectionStrategy);
  }

  /**
   * Remove doctors from a designated body by processing each doctor connection request with GMC.
   *
   * @param removeDoctorDto the connection update request containing doctors to remove
   * @return response indicating success or failure
   */
  public UpdateConnectionResponseDto bulkRemoveConnection(
      final UpdateConnectionDto removeDoctorDto) {
    return processBulkConnectionRequest(removeDoctorDto, removeConnectionStrategy);
  }

  /**
   * Process bulk connection requests by calling GMC for each doctor and handling responses.
   *
   * @param connectionDto the connection update request
   * @param strategy the connection operation strategy to use
   * @return response indicating success or failure
   */
  private UpdateConnectionResponseDto processBulkConnectionRequest(
      final UpdateConnectionDto connectionDto,
      final ConnectionOperationStrategy strategy) {

    log.info("Processing {} doctor request for {} doctor(s)",
        strategy.getOperationType(), connectionDto.getDoctors().size());

    boolean hasFailure = false;
    String firstErrorMessage = null;

    for (DoctorInfoDto doctor : connectionDto.getDoctors()) {
      GmcConnectionResponseDto gmcResponse = strategy.execute(
          gmcClientService,
          doctor,
          connectionDto.getChangeReason(),
          connectionDto.getDesignatedBodyCode()
      );

      GmcResponseCode responseCode = persistAndPublishGmcResponse(
          doctor,
          connectionDto.getChangeReason(),
          connectionDto.getDesignatedBodyCode(),
          gmcResponse,
          strategy.getOperationType(),
          connectionDto.getAdmin()
      );

      if (isErrorResponse(responseCode)) {
        hasFailure = true;
        if (firstErrorMessage == null) {
          firstErrorMessage =
              responseCode != null ? responseCode.getMessage() : "Unknown GMC response";
        }
      }
    }

    if (hasFailure) {
      String errorMessage = getReturnMessage(firstErrorMessage, connectionDto.getDoctors().size());
      return UpdateConnectionResponseDto.builder().message(errorMessage).build();
    }

    return UpdateConnectionResponseDto.builder().message(SUCCESS.getMessage()).build();
  }

  /**
   * Get all connection history for a trainee.
   *
   * @param gmcId gmcId of a trainee
   * @return connection history
   */
  public ConnectionDto getTraineeConnectionInfo(final String gmcId) {
    log.info("Fetching connections info for GmcId: {}", gmcId);
    var connectionDto = new ConnectionDto();
    final var connections = repository.findAllByGmcIdOrderByRequestTimeDesc(gmcId);
    final var allConnectionsForTrainee = connections.stream().map(connection -> {
      var reasonMessage = "";
      var requestType = connection.getRequestType();
      if (requestType != null && requestType.equals(ADD)) {
        reasonMessage = AddConnectionReasonCode.fromCode(connection.getReason());
      } else if (requestType != null && requestType.equals(REMOVE)) {
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
          .updatedBy(connection.getUpdatedBy())
          .build();
    }).collect(toList());
    connectionDto.setConnectionHistory(allConnectionsForTrainee);

    return connectionDto;
  }

  /**
   * Record basic Connection Log for change to Doctor connection.
   *
   * @param connectionLogDto dto containing connection log info
   */
  public void recordConnectionLog(
      ConnectionLogDto connectionLogDto
  ) {
    ConnectionLog log = connectionLogMapper.fromDto(connectionLogDto);
    log.setId(UUID.randomUUID().toString());
    ConnectionLog savedLog = repository.save(log);
    publishConnectionChangedApplicationEvent(savedLog);
  }

  /**
   * Send connection logs to rabbit for elasticsearch sync in pages.
   *
   * @param pageSize the size of each page
   */
  public void sendConnectionLogsForSync(int pageSize) {
    int currentPage = 0;
    Page<ConnectionLog> connectionLogs;

    do {
      connectionLogs = connectionLogCustomRepository.getLatestLogsWithPaging(currentPage, pageSize);
      log.info("Fetched page {} with {} connection logs for sync.", currentPage,
          connectionLogs.getNumberOfElements());
      var syncDataPayload = IndexSyncMessage.builder()
          .payload(connectionLogMapper.toDtoList(connectionLogs.toList())).syncEnd(false).build();
      rabbitTemplate.convertAndSend(exchange, esSyncDataRoutingKey, syncDataPayload);
      currentPage++;
    } while (currentPage < connectionLogs.getTotalPages());

    log.info("Total pages to process for connection logs sync: {}", connectionLogs.getTotalPages());

    var syncEndPayload = IndexSyncMessage.builder().payload(List.of()).syncEnd(true)
        .build();
    rabbitTemplate.convertAndSend(exchange, esSyncDataRoutingKey, syncEndPayload);
  }

  /**
   * Process GMC response by saving logs, publishing events, and sending notifications.
   *
   * @param doctor                the doctor information
   * @param changeReason          the reason for the connection change
   * @param designatedBodyCode    the new designated body code
   * @param gmcResponse           the response from GMC
   * @param connectionRequestType the type of connection request (ADD or REMOVE)
   * @param admin                 the admin user making the request
   * @return the GMC response code
   */
  private GmcResponseCode persistAndPublishGmcResponse(
      final DoctorInfoDto doctor,
      final String changeReason,
      final String designatedBodyCode,
      final GmcConnectionResponseDto gmcResponse,
      final ConnectionRequestType connectionRequestType,
      final String admin) {

    String returnCode = gmcResponse.getReturnCode();
    GmcResponseCode responseCode = fromCode(returnCode);

    // Save additional "External" log if doctor has already been added to this DB
    if (DOCTOR_ALREADY_ASSOCIATED.getCode().equals(returnCode)) {
      saveExternalConnectionLog(doctor.getGmcId(), designatedBodyCode,
          doctor.getCurrentDesignatedBodyCode());
    }

    // Save the main connection request log
    ConnectionRequestLog connectionRequestLog = saveConnectionRequestLog(
        doctor,
        changeReason,
        designatedBodyCode,
        gmcResponse,
        connectionRequestType,
        admin
    );

    // Publish event for successful changes or already-associated outcomes
    if (SUCCESS.equals(responseCode) || DOCTOR_ALREADY_ASSOCIATED.equals(responseCode)) {
      publishConnectionChangedApplicationEvent(connectionRequestLog);
    }

    // Send to rabbit queue or exception logs based on response
    publishNotificationsAndExceptions(doctor.getGmcId(), designatedBodyCode, returnCode,
        gmcResponse.getSubmissionDate(), admin);

    return responseCode;
  }

  /**
   * Save external connection log when doctor is already associated with designated body.
   */
  private void saveExternalConnectionLog(String gmcId, String designatedBodyCode,
      String currentDesignatedBodyCode) {
    repository.save(
        ConnectionRequestLog.builder()
            .gmcId(gmcId)
            .newDesignatedBodyCode(designatedBodyCode)
            .previousDesignatedBodyCode(currentDesignatedBodyCode)
            .updatedBy(UPDATED_BY_GMC)
            .requestTime(now())
            .build()
    );
  }

  /**
   * Save the main connection request log to the database.
   */
  private ConnectionRequestLog saveConnectionRequestLog(
      final DoctorInfoDto doctor,
      final String changeReason,
      final String designatedBodyCode,
      final GmcConnectionResponseDto gmcResponse,
      final ConnectionRequestType connectionRequestType,
      final String admin) {

    ConnectionRequestLog connectionRequestLog = ConnectionRequestLog.builder()
        .id(UUID.randomUUID().toString())
        .gmcId(doctor.getGmcId())
        .gmcClientId(gmcResponse.getGmcRequestId())
        .newDesignatedBodyCode(designatedBodyCode)
        .previousDesignatedBodyCode(doctor.getCurrentDesignatedBodyCode())
        .reason(changeReason)
        .requestType(connectionRequestType)
        .responseCode(gmcResponse.getReturnCode())
        .requestTime(now())
        .updatedBy(admin)
        .build();

    return repository.save(connectionRequestLog);
  }

  /**
   * Handle notifications by sending to rabbit queue or exception logs based on response code.
   */
  private void publishNotificationsAndExceptions(final String gmcId,
      final String designatedBodyCode,
      final String returnCode, final LocalDate submissionDate, final String admin) {

    if (SUCCESS.getCode().equals(returnCode)) {
      sendConnectionMessage(gmcId, designatedBodyCode, submissionDate);
      log.info("Sending message to rabbit to update designated body code");
    } else if (DOCTOR_ALREADY_ASSOCIATED.getCode().equals(returnCode)) {
      sendConnectionMessage(gmcId, designatedBodyCode, submissionDate);
      log.info("Sending message to rabbit for externally made connection");
      String exceptionMessage = DOCTOR_ALREADY_ASSOCIATED.getMessage();
      exceptionService.createExceptionLog(gmcId, exceptionMessage, admin);
    } else {
      GmcResponseCode responseCode = fromCode(returnCode);
      String exceptionMessage = responseCode != null ? responseCode.getMessage() : "Unknown error";
      exceptionService.createExceptionLog(gmcId, exceptionMessage, admin);
    }
  }

  /**
   * Send connection message to rabbit queue.
   */
  private void sendConnectionMessage(String gmcId, String designatedBodyCode,
      LocalDate submissionDate) {
    ConnectionMessage connectionMessage = ConnectionMessage.builder()
        .gmcId(gmcId)
        .designatedBodyCode(designatedBodyCode)
        .submissionDate(submissionDate)
        .gmcLastUpdatedDateTime(now())
        .build();
    rabbitTemplate.convertAndSend(exchange, routingKey, connectionMessage);
  }

  //if bulk request then return generic failure message else gmc error message
  private String getReturnMessage(final String message, final int requestSize) {
    if (requestSize > 1) {
      return "Some changes have failed with GMC, please check the failed GMC updates list";
    }
    return message;
  }

  /**
   * Check if the GMC response code represents an error outcome.
   *
   * @param responseCode the GMC response code to check (may be null)
   * @return false if the response is SUCCESS or DOCTOR_ALREADY_ASSOCIATED, else true
   */
  private boolean isErrorResponse(GmcResponseCode responseCode) {
    return !SUCCESS.equals(responseCode) && !DOCTOR_ALREADY_ASSOCIATED.equals(responseCode);
  }

  private void publishConnectionChangedApplicationEvent(ConnectionLog connectionLog) {
    var event = new ConnectionChangedApplicationEvent(connectionLog);
    applicationEventPublisher.publishEvent(event);
  }
}
