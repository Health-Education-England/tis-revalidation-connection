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
import java.util.stream.Collectors;
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
      ApplicationEventPublisher applicationEventPublisher) {
    this.gmcClientService = gmcClientService;
    this.exceptionService = exceptionService;
    this.repository = repository;
    this.connectionLogCustomRepository = connectionLogCustomRepository;
    this.rabbitTemplate = rabbitTemplate;
    this.connectionLogMapper = connectionLogMapper;
    this.applicationEventPublisher = applicationEventPublisher;
  }

  public UpdateConnectionResponseDto addDoctor(final UpdateConnectionDto addDoctorDto) {
    return processConnectionRequest(addDoctorDto, ADD);
  }

  public UpdateConnectionResponseDto removeDoctor(final UpdateConnectionDto removeDoctorDto) {
    return processConnectionRequest(removeDoctorDto, REMOVE);
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
   * Process a connection request (ADD or REMOVE) for one or more doctors.
   * <p>
   * Flow: 1. Extract request parameters (reason, designated body code) 2. For each doctor: delegate
   * to GMC API and handle the response 3. Check if any requests failed 4. Return appropriate
   * success or error message
   *
   * @param addDoctorDto          the connection update request containing doctors and change
   *                              reason
   * @param connectionRequestType whether to ADD or REMOVE the connection
   * @return response indicating success or failure with appropriate message
   */
  private UpdateConnectionResponseDto processConnectionRequest(
      final UpdateConnectionDto addDoctorDto,
      final ConnectionRequestType connectionRequestType) {
    final var changeReason = addDoctorDto.getChangeReason();
    final var designatedBodyCode = addDoctorDto.getDesignatedBodyCode();

    final var addRemoveResponse = addDoctorDto.getDoctors().stream().map(doctor -> {
      // Step 1: Send the request to GMC API
      final var gmcResponse = delegateRequest(changeReason, designatedBodyCode, doctor,
          connectionRequestType);
      // Step 2: Process GMC response, save logs, and publish events
      return handleGmcResponse(doctor.getGmcId(), changeReason, designatedBodyCode,
          doctor.getCurrentDesignatedBodyCode(), gmcResponse, connectionRequestType,
          addDoctorDto.getAdmin());
    }).collect(Collectors.toList());

    // Check if any of the requests failed
    final var addRemoveResponseFiltered = addRemoveResponse.stream()
        .filter(response -> !response.getMessage().equals(SUCCESS.getMessage())).findAny();

    // If any failures exist, return appropriate error message
    if (addRemoveResponseFiltered.isPresent()) {
      final var errorMessage = getReturnMessage(addRemoveResponseFiltered.get().getMessage(),
          addDoctorDto.getDoctors().size());
      return UpdateConnectionResponseDto.builder().message(errorMessage).build();
    }

    // All requests succeeded
    return UpdateConnectionResponseDto.builder().message(SUCCESS.getMessage()).build();
  }

  /**
   * Delegate the connection request to the appropriate GMC Client API method. Routes to either
   * tryAddDoctor or tryRemoveDoctor based on request type.
   *
   * @param changeReason          the reason for the connection change
   * @param designatedBodyCode    the designated body code to add the doctor to
   * @param doctor                the doctor information
   * @param connectionRequestType ADD or REMOVE
   * @return GMC API response containing return code and request ID
   */
  private GmcConnectionResponseDto delegateRequest(final String changeReason,
      final String designatedBodyCode,
      final DoctorInfoDto doctor, final ConnectionRequestType connectionRequestType) {
    GmcConnectionResponseDto gmcResponse;
    if (ADD == connectionRequestType) {
      gmcResponse = gmcClientService
          .tryAddDoctor(doctor.getGmcId(), changeReason, designatedBodyCode);
    } else {
      gmcResponse = gmcClientService
          .tryRemoveDoctor(doctor.getGmcId(), changeReason, doctor.getCurrentDesignatedBodyCode());
    }
    return gmcResponse;
  }

  /**
   * Handle GMC API response and perform necessary actions.
   * <p>
   * Workflow: 1. Extract response code from GMC
   * 2. If doctor already associated, save an additional "External" log entry
   * 3. Create and save connection request log to the database
   * 4. Publish connection changed event if successful or already associated
   * 5. Send to RabbitMQ or exception logs based on response code
   * 6. Return response DTO with appropriate message
   *
   * @param gmcId                     the GMC ID of the doctor
   * @param changeReason              the reason for the connection change
   * @param designatedBodyCode        the new designated body code
   * @param currentDesignatedBodyCode the previous/current designated body code
   * @param gmcResponse               the response from GMC API
   * @param connectionRequestType     ADD or REMOVE
   * @param admin                     the admin user making the request
   * @return response DTO containing success or error message
   */
  private UpdateConnectionResponseDto handleGmcResponse(final String gmcId,
      final String changeReason,
      final String designatedBodyCode, final String currentDesignatedBodyCode,
      final GmcConnectionResponseDto gmcResponse,
      final ConnectionRequestType connectionRequestType,
      final String admin) {

    var returnCode = gmcResponse.getReturnCode();

    // Special case: Doctor was already associated with this DB (external change)
    // Save additional "External" log to track that the connection was made outside our system
    if (DOCTOR_ALREADY_ASSOCIATED.getCode().equals(returnCode)) {
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

    // Create the main connection request log entry with full details
    final var connectionRequestLog = ConnectionRequestLog.builder()
        .id(UUID.randomUUID().toString())
        .gmcId(gmcId)
        .gmcClientId(gmcResponse.getGmcRequestId())
        .newDesignatedBodyCode(designatedBodyCode)
        .previousDesignatedBodyCode(currentDesignatedBodyCode)
        .reason(changeReason)
        .requestType(connectionRequestType)
        .responseCode(returnCode)
        .requestTime(now())
        .updatedBy(admin)
        .build();

    // Persist the connection request log to Database
    repository.save(connectionRequestLog);

    // Publish application event for successful changes or already-associated outcomes
    // This allows other parts of the system to react to connection changes (show discrepancies)
    if (SUCCESS.getCode().equals(returnCode) || DOCTOR_ALREADY_ASSOCIATED.getCode()
        .equals(returnCode)) {
      publishConnectionChangedApplicationEvent(connectionRequestLog);
    }

    // Send to RabbitMQ for downstream processing or log exception in database if failed
    sendToRabbitOrExceptionLogs(gmcId,
        designatedBodyCode,
        returnCode,
        gmcResponse.getSubmissionDate(),
        admin
    );

    // Convert response code to human-readable message
    final var gmcResponseCode = fromCode(returnCode);
    final var responseMessage = gmcResponseCode != null ? gmcResponseCode.getMessage() : "";
    return UpdateConnectionResponseDto.builder().message(responseMessage).build();
  }

  /**
   * Route the response to either RabbitMQ for downstream processing or exception logs.
   * <p>
   * Logic: - SUCCESS: Send message to RabbitMQ to update doctor's designated body code -
   * DOCTOR_ALREADY_ASSOCIATED: Send to RabbitMQ (external change) AND log exception - Any other
   * error: Log to exception logs only
   *
   * @param gmcId              the GMC ID of the doctor
   * @param designatedBodyCode the designated body code
   * @param returnCode         the GMC API return code
   * @param submissionDate     the date of the submission
   * @param admin              the admin user making the request
   */
  private void sendToRabbitOrExceptionLogs(final String gmcId, final String designatedBodyCode,
      final String returnCode, LocalDate submissionDate, String admin) {
    final String exceptionMessage = GmcResponseCode.fromCode(returnCode).getMessage();

    if (SUCCESS.getCode().equals(returnCode)) {
      // Successful request: Send to RabbitMQ to trigger downstream updates
      final var connectionMessage = ConnectionMessage.builder()
          .gmcId(gmcId)
          .designatedBodyCode(designatedBodyCode)
          .submissionDate(submissionDate)
          .gmcLastUpdatedDateTime(now())
          .build();
      log.info("Sending message to rabbit to update designated body code");
      rabbitTemplate.convertAndSend(exchange, routingKey, connectionMessage);
    } else {
      if (DOCTOR_ALREADY_ASSOCIATED.getCode().equals(returnCode)) {
        // Doctor already associated: Connection was made externally
        // Send to RabbitMQ to sync our system with external change, but also record exception
        final var connectionMessage = ConnectionMessage.builder()
            .gmcId(gmcId)
            .designatedBodyCode(designatedBodyCode)
            .submissionDate(submissionDate)
            .gmcLastUpdatedDateTime(now())
            .build();
        log.info("Sending message to rabbit for externally made connection");
        rabbitTemplate.convertAndSend(exchange, routingKey, connectionMessage);
      }
      // All non-success cases: Log exception for visibility and troubleshooting
      exceptionService.createExceptionLog(gmcId, exceptionMessage, admin);
    }
  }

  /**
   * Determine the appropriate error message based on whether this is a bulk or single request. Bulk
   * requests get a generic message, single requests get the specific GMC error message.
   *
   * @param message     the original GMC error message
   * @param requestSize the number of doctors in the request
   * @return appropriate error message for the response
   */
  private String getReturnMessage(final String message, final int requestSize) {
    if (requestSize > 1) {
      // Bulk request: Return generic message to avoid confusion
      return "Some changes have failed with GMC, please check the failed GMC updates list";
    }
    // Single request: Return specific GMC error message
    return message;
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

  private void publishConnectionChangedApplicationEvent(ConnectionLog connectionLog) {
    var event = new ConnectionChangedApplicationEvent(connectionLog);
    applicationEventPublisher.publishEvent(event);
  }
}
