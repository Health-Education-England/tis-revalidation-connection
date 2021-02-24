package uk.nhs.hee.tis.revalidation.connection.message;


import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionView;
import uk.nhs.hee.tis.revalidation.connection.service.ElasticSearchIndexUpdateHelper;
import uk.nhs.hee.tis.revalidation.connection.service.ElasticSearchService;


@Slf4j
@Component
public class RabbitMessageListener {

  @Autowired
  private ElasticSearchService elasticSearchService;

  @Autowired
  private ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper;

  @RabbitListener(queues = "${app.rabbit.es-queue}")
  public void receiveMessage(final ConnectionInfoDto connectionInfo) {
    log.info("MESSAGE RECEIVED: " + connectionInfo);
    elasticSearchIndexUpdateHelper.updateElasticSearchIndex(connectionInfo);
  }

  @RabbitListener(queues = "${app.rabbit.es-connections-queue}")
  public void receiveConnectionMessage(final ConnectionInfoDto connectionInfo) {
    log.info("CONNECTIONS MESSAGE RECEIVED: " + connectionInfo);
    elasticSearchService.addExceptionViews(
        elasticSearchIndexUpdateHelper.getExceptionViews(connectionInfo)
    );
  }

}
