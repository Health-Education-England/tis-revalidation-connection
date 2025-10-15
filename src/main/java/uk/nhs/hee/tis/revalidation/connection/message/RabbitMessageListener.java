package uk.nhs.hee.tis.revalidation.connection.message;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionLogDto;
import uk.nhs.hee.tis.revalidation.connection.service.ConnectionService;

@Component
public class RabbitMessageListener {

  private ConnectionService connectionService;

  public RabbitMessageListener(ConnectionService connectionService) {
    this.connectionService = connectionService;
  }

  @RabbitListener(queues = "${app.rabbit.reval.queue.connection.log}")
  public void receiveConnectionLog(ConnectionLogDto connectionLogDto) {
    connectionService.recordConnectionLog(connectionLogDto);
  }
}
