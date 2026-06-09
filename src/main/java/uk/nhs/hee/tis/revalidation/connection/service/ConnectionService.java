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
import uk.nhs.hee.tis.revalidation.connection.dto.GmcConnectionResponseDto;
import uk.nhs.hee.tis.revalidation.connection.dto.UpdateConnectionDto;
import uk.nhs.hee.tis.revalidation.connection.dto.UpdateConnectionResponseDto;
import uk.nhs.hee.tis.revalidation.connection.entity.AddConnectionReasonCode;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionLog;
import uk.nhs.hee.tis.revalidation.connection.context.ConnectionRequestContext;
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
    return processUpdateConnectionRequest(addDoctorDto, ADD);
  }

  public UpdateConnectionResponseDto removeDoctor(final UpdateConnectionDto removeDoctorDto) {
    return processUpdateConnectionRequest(removeDoctorDto, REMOVE);
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

  private UpdateConnectionResponseDto processUpdateConnectionRequest(
      final UpdateConnectionDto updateConnectionDto,
      final ConnectionRequestType connectionRequestType) {

    // Convert bulk request into individual request contexts
    final List<ConnectionRequestContext> requestContexts = buildConnectionRequestContexts(
        updateConnectionDto,
        connectionRequestType);

    // Make requests to GMC and record and publish the responses
    final List<UpdateConnectionResponseDto> updateConnectionResponses = requestContexts.stream()
        .map(ctx -> {
          try {
            final GmcConnectionResponseDto gmcResponse = delegateRequestToGmcApi(ctx);
            return recordAndPublishGmcResponse(ctx, gmcResponse);

          } catch (IllegalArgumentException e) {
            return UpdateConnectionResponseDto.builder()
                .message("Validation error: " + e.getMessage())
                .build();
          }
        }).collect(Collectors.toList());

    // Check for errors to determine response body
    final var errorResponses = updateConnectionResponses.stream()
        .filter(response -> !SUCCESS.getMessage().equals(response.getMessage())).findAny();

    if (errorResponses.isPresent()) {
      final var errorMessage = getReturnMessage(errorResponses.get().getMessage(),
          updateConnectionDto.getDoctors().size());

      return UpdateConnectionResponseDto.builder().message(errorMessage).build();
    }
    return UpdateConnectionResponseDto.builder().message(SUCCESS.getMessage()).build();
  }

  private List<ConnectionRequestContext> buildConnectionRequestContexts(
      final UpdateConnectionDto connectionDto,
      final ConnectionRequestType requestType) {
    return connectionDto.getDoctors().stream()
        .map(doctor -> ConnectionRequestContext.builder()
            .changeReason(connectionDto.getChangeReason())
            .designatedBodyCode(connectionDto.getDesignatedBodyCode())
            .doctor(doctor)
            .connectionRequestType(requestType)
            .admin(connectionDto.getAdmin())
            .build())
        .collect(Collectors.toList());
  }

  //delegate request to GMC Client
  private GmcConnectionResponseDto delegateRequestToGmcApi(final ConnectionRequestContext context) {
    final ConnectionRequestType requestType = context.getConnectionRequestType();

    if (requestType == null) {
      throw new IllegalArgumentException("Connection request type cannot be null");
    }

    switch (requestType) {
      case ADD:
        return gmcClientService.tryAddDoctor(
            context.getDoctor().getGmcId(),
            context.getChangeReason(),
            context.getDesignatedBodyCode()
        );
      case REMOVE:
        return gmcClientService.tryRemoveDoctor(
            context.getDoctor().getGmcId(),
            context.getChangeReason(),
            context.getDoctor().getCurrentDesignatedBodyCode()
        );
      default:
        throw new IllegalArgumentException(
            "Unsupported connection request type: " + requestType
        );
    }
  }

  // Record Connection/Exception Logs and publish changes to rabbit
  private UpdateConnectionResponseDto recordAndPublishGmcResponse(
      final ConnectionRequestContext context,
      final GmcConnectionResponseDto response) {

    final String returnCode = response.getReturnCode();

    // Save additional "External" log if doctor has already been added to this DB
    if (DOCTOR_ALREADY_ASSOCIATED.getCode().equals(returnCode)) {
      repository.save(buildConnectionRequestLog(context, null, UPDATED_BY_GMC, null, false));
    }

    // Save main connection request log
    final var connectionRequestLog = buildConnectionRequestLog(
        context, returnCode, context.getAdmin(), response.getGmcRequestId(), true);
    repository.save(connectionRequestLog);

    // Publish event for successful connections
    if (shouldPublishEvent(returnCode)) {
      publishConnectionChangedApplicationEvent(connectionRequestLog);
    }

    // Send to RabbitMQ or exception logs
    sendToRabbitOrExceptionLogs(
        context.getDoctor().getGmcId(),
        context.getDesignatedBodyCode(),
        returnCode,
        response.getSubmissionDate(),
        context.getAdmin()
    );

    // Build and return response
    final var gmcResponseCode = fromCode(returnCode);
    final var responseMessage = gmcResponseCode != null ? gmcResponseCode.getMessage() : "";
    return UpdateConnectionResponseDto.builder().message(responseMessage).build();
  }

  // If success, send message to RabbitMQ to update doctors; otherwise log exception
  private void sendToRabbitOrExceptionLogs(final String gmcId, final String designatedBodyCode,
      final String returnCode, final LocalDate submissionDate, final String admin) {

    // Send to RabbitMQ for successful connections or already-associated doctors
    if (shouldSendToRabbit(returnCode)) {
      final var connectionMessage = buildConnectionMessage(gmcId, designatedBodyCode, submissionDate);
      final var logMessage = SUCCESS.getCode().equals(returnCode)
          ? "Sending message to rabbit to update designated body code"
          : "Sending message to rabbit for externally made connection";
      log.info(logMessage);
      rabbitTemplate.convertAndSend(exchange, routingKey, connectionMessage);
    }

    // Log exception for any non-success response
    if (!SUCCESS.getCode().equals(returnCode)) {
      final var gmcResponseCode = GmcResponseCode.fromCode(returnCode);
      final String exceptionMessage = gmcResponseCode != null ? gmcResponseCode.getMessage() : "Unknown error";
      exceptionService.createExceptionLog(gmcId, exceptionMessage, admin);
    }
  }

  //if bulk request then return generic failure message else gmc error message
  private String getReturnMessage(final String message, final int requestSize) {
    if (requestSize > 1) {
      return "Some changes have failed with GMC, please check the failed GMC updates list";
    }
    return message;
  }

  private void publishConnectionChangedApplicationEvent(ConnectionLog connectionLog) {
    var event = new ConnectionChangedApplicationEvent(connectionLog);
    applicationEventPublisher.publishEvent(event);
  }

  private ConnectionRequestLog buildConnectionRequestLog(
      final ConnectionRequestContext context,
      final String responseCode,
      final String admin,
      final String gmcClientId,
      final boolean includeReasonAndRequestType) {
    return ConnectionRequestLog.builder()
        .id(UUID.randomUUID().toString())
        .gmcId(context.getDoctor().getGmcId())
        .gmcClientId(gmcClientId)
        .newDesignatedBodyCode(context.getDesignatedBodyCode())
        .previousDesignatedBodyCode(context.getDoctor().getCurrentDesignatedBodyCode())
        .reason(includeReasonAndRequestType ? context.getChangeReason() : null)
        .requestType(includeReasonAndRequestType ? context.getConnectionRequestType() : null)
        .responseCode(responseCode)
        .requestTime(now())
        .updatedBy(admin)
        .build();
  }

  private ConnectionMessage buildConnectionMessage(
      final String gmcId,
      final String designatedBodyCode,
      final LocalDate submissionDate) {
    return ConnectionMessage.builder()
        .gmcId(gmcId)
        .designatedBodyCode(designatedBodyCode)
        .submissionDate(submissionDate)
        .gmcLastUpdatedDateTime(now())
        .build();
  }

  private boolean shouldPublishEvent(final String returnCode) {
    return SUCCESS.getCode().equals(returnCode)
        || DOCTOR_ALREADY_ASSOCIATED.getCode().equals(returnCode);
  }

  private boolean shouldSendToRabbit(final String returnCode) {
    return SUCCESS.getCode().equals(returnCode)
        || DOCTOR_ALREADY_ASSOCIATED.getCode().equals(returnCode);
  }
}
