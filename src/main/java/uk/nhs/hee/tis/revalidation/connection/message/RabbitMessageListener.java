package uk.nhs.hee.tis.revalidation.connection.message;


import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.service.ElasticSearchIndexUpdateHelper;
import uk.nhs.hee.tis.revalidation.connection.service.UpdateExceptionElasticSearchService;


@Slf4j
@Component
public class RabbitMessageListener {

  @Autowired
  private UpdateExceptionElasticSearchService updateExceptionElasticSearchService;

  @Autowired
  private ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper;

  @RabbitListener(queues = "${app.rabbit.es.queue}")
  public void receiveMessage(final ConnectionInfoDto connectionInfo) {
    log.info("MESSAGE RECEIVED: " + connectionInfo);
    elasticSearchIndexUpdateHelper.updateElasticSearchIndex(connectionInfo);
  }

}
