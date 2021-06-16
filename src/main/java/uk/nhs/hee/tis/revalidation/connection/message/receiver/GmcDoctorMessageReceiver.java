package uk.nhs.hee.tis.revalidation.connection.message.receiver;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.GmcDoctor;
import uk.nhs.hee.tis.revalidation.connection.entity.MasterDoctorView;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.MasterElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.connection.service.ElasticSearchIndexUpdateHelper;
import uk.nhs.hee.tis.revalidation.connection.service.MasterElasticSearchService;

@Component
public class GmcDoctorMessageReceiver implements MessageReceiverBase<GmcDoctor> {

  private ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper;

  private MasterElasticSearchService masterElasticSearchService;

  private MasterElasticSearchRepository masterElasticSearchRepository;

  private ConnectionInfoMapper connectionInfoMapper;

  /**
   * Class to handle gmc doctor update messages
   *
   * @param elasticSearchIndexUpdateHelper
   * @param masterElasticSearchService
   * @param masterElasticSearchRepository
   * @param connectionInfoMapper
   */
  public GmcDoctorMessageReceiver(
      ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper,
      MasterElasticSearchService masterElasticSearchService,
      MasterElasticSearchRepository masterElasticSearchRepository,
      ConnectionInfoMapper connectionInfoMapper
  ) {
    this.elasticSearchIndexUpdateHelper = elasticSearchIndexUpdateHelper;
    this.masterElasticSearchService = masterElasticSearchService;
    this.masterElasticSearchRepository = masterElasticSearchRepository;
    this.connectionInfoMapper = connectionInfoMapper;
  }

  /**
   * Handles gmc doctor update messages
   *
   * @param message message containing GmcDoctor
   */
  @Override
  public void handleMessage(GmcDoctor message) {
    List<MasterDoctorView> existingViews = masterElasticSearchRepository
        .findByGmcReferenceNumber(message.getGmcReferenceNumber());
    List<ConnectionInfoDto> updatedConnections = updateExistingViews(existingViews, message);
    updatedConnections.forEach(connection -> {
      masterElasticSearchService.updateMasterIndex(connection);
      elasticSearchIndexUpdateHelper.updateElasticSearchIndex(connection);
    });
  }

  private List<ConnectionInfoDto> updateExistingViews(
      List<MasterDoctorView> existingViews,
      GmcDoctor doctor
  ) {
    List<ConnectionInfoDto> connectionInfoDtos = new ArrayList<>();
    existingViews.forEach(existingView -> {
      existingView.setDoctorFirstName(doctor.getDoctorFirstName());
      existingView.setDoctorLastName(doctor.getDoctorLastName());
      existingView.setDesignatedBody(doctor.getDesignatedBodyCode());
      connectionInfoDtos.add(connectionInfoMapper.masterToDto(existingView));
    });
    return connectionInfoDtos;
  }
}
