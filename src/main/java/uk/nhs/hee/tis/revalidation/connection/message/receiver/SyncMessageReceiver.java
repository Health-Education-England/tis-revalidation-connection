package uk.nhs.hee.tis.revalidation.connection.message.receiver;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.connection.config.ElasticsearchConfig;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.service.ConnectionService;
import uk.nhs.hee.tis.revalidation.connection.service.ElasticSearchIndexUpdateHelper;
import uk.nhs.hee.tis.revalidation.connection.service.MasterElasticSearchService;

@Slf4j
@Component
public class SyncMessageReceiver extends MessageReceiverBase<String>{

  private ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper;

  private MasterElasticSearchService masterElasticSearchService;

  public SyncMessageReceiver (
      ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper,
      MasterElasticSearchService masterElasticSearchService
  ) {
    this.elasticSearchIndexUpdateHelper = elasticSearchIndexUpdateHelper;
    this.masterElasticSearchService = masterElasticSearchService;
  }

  @Override
  public void handleMessage(final String getMaster) {
    if (getMaster != null && getMaster.equals("getMaster")) {

      //Delete and create elastic search index
      elasticSearchIndexUpdateHelper.clearConnectionIndexes(ElasticsearchConfig.ES_INDICES);

      final List<ConnectionInfoDto> masterList = masterElasticSearchService.findAllScroll();
      log.info("Found {} records from ES Master index. ", masterList.size());

      masterList.forEach(connectionInfo ->
          elasticSearchIndexUpdateHelper.updateElasticSearchIndex(connectionInfo));
      log.info("ES indexes update completed.");
    }
  }
}
