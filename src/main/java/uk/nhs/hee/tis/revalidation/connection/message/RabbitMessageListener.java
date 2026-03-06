package uk.nhs.hee.tis.revalidation.connection.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionLogDto;
import uk.nhs.hee.tis.revalidation.connection.service.ConnectionService;
import uk.nhs.hee.tis.revalidation.connection.service.HiddenDiscrepancyService;

@Slf4j
@Component
public class RabbitMessageListener {

  protected static final String CONNECTION_LOG_SYNC_START_MESSAGE = "connectionLogSyncStart";
  private final ConnectionService connectionService;
  private final HiddenDiscrepancyService hiddenDiscrepancyService;

  @Value("${app.reval.essync.connectionlog.batchsize}")
  protected int connectionLogBatchSize;

  @Value("${app.reval.essync.hiddendiscrepancy.batchsize}")
  protected int hiddenDiscrepancyBatchSize;

  public RabbitMessageListener(ConnectionService connectionService,
      HiddenDiscrepancyService hiddenDiscrepancyService) {
    this.connectionService = connectionService;
    this.hiddenDiscrepancyService = hiddenDiscrepancyService;
  }

  @RabbitListener(queues = "${app.rabbit.reval.queue.connection.connectionlog}")
  public void receiveConnectionLog(ConnectionLogDto connectionLogDto) {
    connectionService.recordConnectionLog(connectionLogDto);
  }

  /**
   * Listens to messages from integration service to start connectionLog data sync to ES.
   *
   * @param esSyncStart the message to start connectionLog data sync
   */
  @RabbitListener(queues = "${app.rabbit.reval.queue.connectionlog.essyncstart}", ackMode = "NONE")
  public void connectionLogEsSync(final String esSyncStart) {
    log.info("Message from integration service to start connectionLog data sync: {}",
        esSyncStart);

    if (!CONNECTION_LOG_SYNC_START_MESSAGE.equals(esSyncStart)) {
      log.warn("Received invalid message to start connectionLog ES sync.");
      return;
    }
    connectionService.sendConnectionLogsForSync(connectionLogBatchSize);
  }

  /**
   * Listens to messages from integration service to start hidden discrepancy data sync to ES.
   *
   * @param esSyncStart the message to start hidden discrepancy data sync
   */
  @RabbitListener(queues = "${app.rabbit.reval.queue.hiddenDiscrepancy.essyncstart}", ackMode = "NONE")
  public void hiddenDiscrepancyEsSync(final String esSyncStart) {
    log.info("Message from integration service to start hiddenDiscrepancy data sync: {}",
        esSyncStart);

    if (!CONNECTION_LOG_SYNC_START_MESSAGE.equals(esSyncStart)) {
      log.warn("Received invalid message to start hiddenDiscrepancy ES sync.");
      return;
    }
    hiddenDiscrepancyService.sendHiddenDiscrepanciesForSync(hiddenDiscrepancyBatchSize);
  }
}
