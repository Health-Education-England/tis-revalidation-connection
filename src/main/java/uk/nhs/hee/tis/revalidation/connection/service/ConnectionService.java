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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.context.ConnectionRequestContext;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionDto;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionHistoryDto;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionLogDto;
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

  private final ConnectionRepository connectionRepository;

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
  private static final String CONNECTION_UPDATE_ALL_SUCCESSFUL_MESSAGE
      = "All connection requests processed successfully.";
  private static final String CONNECTION_UPDATE_MULTIPLE_ERROR_MESSAGE
      = "Some connection requests have failed, please check the Failed GMC Updates list";
  private static final String CONNECTION_UPDATE_UNSUPPORTED_ACTION_MESSAGE
      = "Error: Unsupported connection request type provided.";


  public ConnectionService(GmcClientService gmcClientService, ExceptionLogService exceptionService,
      ConnectionRepository connectionRepository,
      ConnectionLogCustomRepository connectionLogCustomRepository,
      RabbitTemplate rabbitTemplate,
      ConnectionLogMapper connectionLogMapper,
      ApplicationEventPublisher applicationEventPublisher) {
    this.gmcClientService = gmcClientService;
    this.exceptionService = exceptionService;
    this.connectionRepository = connectionRepository;
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
    final var connections = connectionRepository.findAllByGmcIdOrderByRequestTimeDesc(gmcId);
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
    ConnectionLog connectionLog = connectionLogMapper.fromDto(connectionLogDto);
    connectionLog.setId(UUID.randomUUID().toString());
    ConnectionLog savedLog = connectionRepository.save(connectionLog);
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

    final List<ConnectionRequestContext> requestContexts = buildConnectionRequestContexts(
        updateConnectionDto,
        connectionRequestType);

    final List<Optional<String>> responses = requestContexts.stream()
        .map(ctx -> {
          try {
            final GmcConnectionResponseDto gmcResponse = delegateRequestToGmcApi(ctx);
            return processAndPublishGmcResponse(ctx, gmcResponse);

          } catch (IllegalArgumentException e) {
            log.error("Error processing connection request for GMC ID {}: {}",
                ctx.getDoctor().getGmcId(), e.getMessage());
            return Optional.of(CONNECTION_UPDATE_UNSUPPORTED_ACTION_MESSAGE);
          }
        }).filter(Optional::isPresent).collect(toList());

    String responseMessage = "";
    if (responses.isEmpty()) {
      responseMessage = CONNECTION_UPDATE_ALL_SUCCESSFUL_MESSAGE;
    } else if (responses.size() > 1) {
      responseMessage = CONNECTION_UPDATE_MULTIPLE_ERROR_MESSAGE;
    } else {
      responseMessage = responses.get(0).get();
    }

    return UpdateConnectionResponseDto.builder()
        .message(responseMessage)
        .build();
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
            .requestDateTime(now())
            .admin(connectionDto.getAdmin())
            .build())
        .collect(Collectors.toList());
  }

  //delegate request to GMC Client
  private GmcConnectionResponseDto delegateRequestToGmcApi(final ConnectionRequestContext context) {
    final var requestType = context.getConnectionRequestType();

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

  private Optional<String> processAndPublishGmcResponse(
      final ConnectionRequestContext context,
      final GmcConnectionResponseDto response) {

    final GmcResponseCode responseCode = GmcResponseCode.fromCode(response.getReturnCode());

    if (responseCode == null) {
      return Optional.of(
          "Error: Received unknown response code from GMC: " + response.getReturnCode());
    }

    switch (responseCode) {
      case SUCCESS:
        var successLog = saveConnectionLogForConnectionRequest(context, response);
        publishConnectionChangedApplicationEvent(successLog);
        publishConnectionUpdatedMessage(successLog, response.getSubmissionDate());
        return Optional.empty();
      case DOCTOR_ALREADY_ASSOCIATED:
        // Save an additional log to show update was made externally to the system
        saveConnectionLogForConnectionRequest(context, response, UPDATED_BY_GMC);
        var associatedLog = saveConnectionLogForConnectionRequest(context, response);
        publishConnectionChangedApplicationEvent(associatedLog);
        publishConnectionUpdatedMessage(associatedLog, response.getSubmissionDate());
        exceptionService.createExceptionLog(context.getDoctor().getGmcId(),
            responseCode.getMessage(), context.getAdmin());
        return Optional.of(responseCode.getMessage());
      default: // Other conditions are exceptional
        saveConnectionLogForConnectionRequest(context, response);
        exceptionService.createExceptionLog(context.getDoctor().getGmcId(),
            responseCode.getMessage(), context.getAdmin());
        return Optional.of(responseCode.getMessage());
    }
  }

  private void publishConnectionChangedApplicationEvent(ConnectionLog connectionLog) {
    var event = new ConnectionChangedApplicationEvent(connectionLog);
    applicationEventPublisher.publishEvent(event);
  }

  private ConnectionLog saveConnectionLogForConnectionRequest(ConnectionRequestContext context,
      GmcConnectionResponseDto response) {
    return saveConnectionLogForConnectionRequest(context, response, context.getAdmin());
  }

  private ConnectionLog saveConnectionLogForConnectionRequest(ConnectionRequestContext context,
      GmcConnectionResponseDto response, String updatedBy) {
    String reason = !UPDATED_BY_GMC.equals(updatedBy) ? context.getChangeReason() : null;
    String responseCode = !UPDATED_BY_GMC.equals(updatedBy) ? response.getReturnCode() : null;
    final var log = ConnectionRequestLog.builder()
        .id(UUID.randomUUID().toString())
        .gmcId(context.getDoctor().getGmcId())
        .gmcClientId(response.getGmcRequestId())
        .newDesignatedBodyCode(context.getDesignatedBodyCode())
        .previousDesignatedBodyCode(context.getDoctor().getCurrentDesignatedBodyCode())
        .reason(reason)
        .requestType(context.getConnectionRequestType())
        .responseCode(responseCode)
        .requestTime(context.getRequestDateTime())
        .updatedBy(updatedBy)
        .build();
    return connectionRepository.save(log);
  }

  private void publishConnectionUpdatedMessage(ConnectionLog log, LocalDate submissionDate) {
    var message = ConnectionMessage.builder()
        .gmcId(log.getGmcId())
        .designatedBodyCode(log.getNewDesignatedBodyCode())
        .submissionDate(submissionDate)
        .gmcLastUpdatedDateTime(log.getRequestTime())
        .build();
    rabbitTemplate.convertAndSend(exchange, routingKey, message);
  }
}
