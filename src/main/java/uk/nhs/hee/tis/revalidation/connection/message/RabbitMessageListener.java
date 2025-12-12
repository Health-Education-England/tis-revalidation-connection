package uk.nhs.hee.tis.revalidation.connection.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionLogDto;
import uk.nhs.hee.tis.revalidation.connection.service.ConnectionService;

@Slf4j
@Component
public class RabbitMessageListener {

  protected static final String CONNECTION_LOG_SYNC_START_MESSAGE = "connectionLogSyncStart";
  private final ConnectionService connectionService;

  @Value("${app.reval.essync.batchsize}")
  protected int batchSize;

  public RabbitMessageListener(ConnectionService connectionService) {
    this.connectionService = connectionService;
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
    connectionService.sendConnectionLogsForSync(batchSize);
  }
}
