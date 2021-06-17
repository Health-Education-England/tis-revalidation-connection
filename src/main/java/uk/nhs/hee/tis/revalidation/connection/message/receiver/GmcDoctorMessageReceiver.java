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
public class GmcDoctorMessageReceiver implements MessageReceiver<GmcDoctor> {

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
