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

  public UpdateConnectionResponseDto addDoctors(final UpdateConnectionDto addDoctorsDto) {
    return processConnectionRequest(addDoctorsDto, ADD);
  }

  public UpdateConnectionResponseDto removeDoctors(final UpdateConnectionDto removeDoctorsDto) {
    return processConnectionRequest(removeDoctorsDto, REMOVE);
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
  public void recordConnectionLog(ConnectionLogDto connectionLogDto) {
    ConnectionLog log = connectionLogMapper.fromDto(connectionLogDto);
    log.setId(UUID.randomUUID().toString());
    ConnectionLog savedLog = repository.save(log);
    publishConnectionChangedApplicationEvent(savedLog);
  }

  /**
   * Process a connection request (ADD or REMOVE) for one or more doctors. This method is
   * responsible for aggregating updates, with minimal mapping to individual requests.
   *
   * <p>Flow:
   * <ol>
   *   <li>For each doctor: delegate to GMC API and handle the response</li>
   *   <li>Check if any requests failed</li>
   *   <li>Return appropriate success or error message</li>
   * </ol>
   *
   * @param bulkRequestDto        the connection update request containing doctors and change
   *                              reason
   * @param connectionRequestType whether to ADD or REMOVE the connection
   * @return response indicating success or failure with appropriate message
   */
  private UpdateConnectionResponseDto processConnectionRequest(
      final UpdateConnectionDto bulkRequestDto,
      final ConnectionRequestType connectionRequestType) {

    final var responseCodes = bulkRequestDto.getDoctors().stream()
        .map(doctor -> {
          ConnectionRequestLog connectionRequestLog = ConnectionRequestLog.builder()
              .id(UUID.randomUUID().toString())
              .gmcId(doctor.getGmcId())
              .newDesignatedBodyCode(bulkRequestDto.getDesignatedBodyCode())
              .previousDesignatedBodyCode(doctor.getCurrentDesignatedBodyCode())
              .reason(bulkRequestDto.getChangeReason())
              .requestType(connectionRequestType)
              .requestTime(now())
              .updatedBy(bulkRequestDto.getAdmin())
              .build();
          return changeDoctorConnection(bulkRequestDto, connectionRequestLog);
        })
        .collect(Collectors.toSet());

    Optional<GmcResponseCode> firstFail = responseCodes.stream()
        .filter(code -> !SUCCESS.equals(code))
        .findAny();
    if (firstFail.isEmpty()) {
      return UpdateConnectionResponseDto.builder().message(SUCCESS.getMessage()).build();
    }
    if (bulkRequestDto.getDoctors().size() == 1) {
      // A request for a single doctor failed
      return UpdateConnectionResponseDto.builder().message(firstFail.get().getMessage()).build();
    }
    return UpdateConnectionResponseDto.builder()
        .message("Some changes have failed with GMC. Please check the failed GMC updates list")
        .build();
  }

  /**
   * Process a connection request (ADD or REMOVE) for a doctor. This method is responsible for
   * controlling the external save and initiating actions to make TIS-Revalidation data consistent
   * with the result of the request.
   *
   * <p>Flow:
   * <ol>
   *   <li>For each doctor: delegate to GMC API</li>
   *   <li>Use the response from the GMC to determine what actions should be taken:
   *     <ul>
   *       <li>When the request is to `ADD`, record when the doctor is already connected</li>
   *       <li>Always save a record of the request</li>
   *       <li>When a change to the GMC fails, save a Failed Update as an
   *        {@link uk.nhs.hee.tis.revalidation.connection.entity.ExceptionLog ExceptionLog}</li>
   *       <li>When the connection has changed*, publish the message</li>
   *     </ul>
   *   </li>
   *   <li>Provide a @link{Nullable} </li>
   * </ol>
   *
   * <p>*N.B. There is currently the assumption that a doctor who is already connected has had their
   * designated body changed.  This is not always true.
   *
   * @param bulkRequestDto       the user request with shared attributes
   * @param connectionRequestLog the request for an individual doctor without response attributes
   * @return the response code from the GMC.  Null where the response code is not recognised.
   */
  private GmcResponseCode changeDoctorConnection(UpdateConnectionDto bulkRequestDto,
      ConnectionRequestLog connectionRequestLog) {
    /*
     * Delegate the connection request to the appropriate GMC Client API method. Routes to
     * either tryAddDoctor or tryRemoveDoctor based on request type.
     */
    GmcConnectionResponseDto gmcResponse;
    if (ADD.equals(connectionRequestLog.getRequestType())) {
      gmcResponse = gmcClientService.tryAddDoctor(connectionRequestLog.getGmcId(),
          bulkRequestDto.getChangeReason(),
          connectionRequestLog.getNewDesignatedBodyCode());
    } else {
      gmcResponse = gmcClientService.tryRemoveDoctor(connectionRequestLog.getGmcId(),
          bulkRequestDto.getChangeReason(),
          connectionRequestLog.getPreviousDesignatedBodyCode());
    }

    connectionRequestLog.setGmcClientId(gmcResponse.getGmcRequestId());
    connectionRequestLog.setResponseCode(gmcResponse.getReturnCode());

    final var returnCodeString = gmcResponse.getReturnCode();
    if (DOCTOR_ALREADY_ASSOCIATED.getCode().equals(returnCodeString)) {
      /*
       * Special case: Doctor was already associated with this DB (external change)
       * Save additional "External" log to track that the connection was made outside our system
       * N.B. The DBC has not necessarily changed.
       * It is possible that the intention was to save a `ConnectionLog`, missed from TIS21-7864,
       *  following commit 0a574bed4adac6976ea6282f905b8120c5626004.
       */
      repository.save(ConnectionRequestLog.builder()
          .gmcId(connectionRequestLog.getGmcId())
          .newDesignatedBodyCode(bulkRequestDto.getDesignatedBodyCode())
          .previousDesignatedBodyCode(connectionRequestLog.getPreviousDesignatedBodyCode())
          .updatedBy(UPDATED_BY_GMC)
          .requestTime(now())
          .build());
    }
    // Persist the connection request log to Database
    repository.save(connectionRequestLog);

    // Convert response code to human-readable message
    GmcResponseCode responseCode = fromCode(returnCodeString);
    final String responseMessage = responseCode == null ? "" : responseCode.getMessage();

    if (!SUCCESS.getCode().equals(returnCodeString)) {
      // Save Failed Update `ExceptionLog` for visibility and troubleshooting
      exceptionService.createExceptionLog(connectionRequestLog.getGmcId(), responseMessage,
          bulkRequestDto.getAdmin());
    }

    /*
     * Publish application event for successful changes or already-associated outcomes
     * This allows other parts of the system to react to connection changes (show discrepancies)
     */
    if (SUCCESS.getCode().equals(returnCodeString)
        || DOCTOR_ALREADY_ASSOCIATED.getCode().equals(returnCodeString)) {
      publishConnectionChangedApplicationEvent(connectionRequestLog);
      final var connectionMessage = ConnectionMessage.builder()
          .gmcId(connectionRequestLog.getGmcId())
          .designatedBodyCode(bulkRequestDto.getDesignatedBodyCode())
          .submissionDate(gmcResponse.getSubmissionDate())
          .gmcLastUpdatedDateTime(now())
          .build();
      log.info("Sending message to rabbit to update designated body code");
      rabbitTemplate.convertAndSend(exchange, routingKey, connectionMessage);
    }

    return responseCode;
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
