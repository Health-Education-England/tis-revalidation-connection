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

  private ConnectionService connectionService;

  @Value("${app.reval.essync.batchsize}")
  private int batchSize;

  public RabbitMessageListener(ConnectionService connectionService) {
    this.connectionService = connectionService;
  }

  @RabbitListener(queues = "${app.rabbit.reval.queue.connection.connectionlog}")
  public void receiveConnectionLog(ConnectionLogDto connectionLogDto) {
    connectionService.recordConnectionLog(connectionLogDto);
  }

  @RabbitListener(queues = "${app.rabbit.reval.queue.connectionlog.essyncstart}", ackMode = "NONE")
  public void connectionLogEsSync(final String esSyncStart) {
    log.info("Message from integration service to start connectionLog data sync: {}", esSyncStart);

    if (esSyncStart == null || !"esSyncStart".equals(esSyncStart)) {
      log.warn("Received invalid message to start connectionLog ES sync.");
      return;
    }

    connectionService.getConnectionLogsForSync(batchSize);

  }
}
