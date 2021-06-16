package uk.nhs.hee.tis.revalidation.connection.message.receiver;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.connection.service.ConnectionService;
import uk.nhs.hee.tis.revalidation.connection.service.ElasticSearchIndexUpdateHelper;
import uk.nhs.hee.tis.revalidation.connection.service.MasterElasticSearchService;

@Slf4j
@Component
public class ConnectionMessageReceiver extends MessageReceiverBase<ConnectionInfoDto> {

  private ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper;

  private MasterElasticSearchService masterElasticSearchService;

  private ConnectionService connectionService;

  public ConnectionMessageReceiver (
      ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper,
      MasterElasticSearchService masterElasticSearchService,
      ConnectionService connectionService
  ) {
    this.elasticSearchIndexUpdateHelper = elasticSearchIndexUpdateHelper;
    this.masterElasticSearchService = masterElasticSearchService;
    this.connectionService = connectionService;
  }

  @Override
  public void handleMessage(ConnectionInfoDto message) {
    log.debug("MESSAGE RECEIVED: " + message);

    // TODO: change to get data from ES 'Master' index instead of mongoDB
    //  when 'Master' index is implemented
    // Get Gmc data and aggregate it to connectionInfo
    if (message.getGmcReferenceNumber() != null) {
      Optional<DoctorsForDB> optionalGmcData = connectionService
          .getDoctorsForDbByGmcId(message.getGmcReferenceNumber());
      if (optionalGmcData.isPresent()) {
        final DoctorsForDB gmcData = optionalGmcData.get();
        message.setSubmissionDate(gmcData.getSubmissionDate());
        message.setDesignatedBody(gmcData.getDesignatedBodyCode());
      }
    }
    try {
      masterElasticSearchService.updateMasterIndex(message);
      elasticSearchIndexUpdateHelper.updateElasticSearchIndex(message);
    } catch (Exception e) {
      log.info("Exception in receiveMessageUpdate: {}", e.getMessage());
    }
  }
}
