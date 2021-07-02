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

import static uk.nhs.hee.tis.revalidation.connection.entity.GmcResponseCode.SUCCESS;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.MasterDoctorView;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.MasterElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.connection.service.ElasticSearchIndexUpdateHelper;
import uk.nhs.hee.tis.revalidation.connection.service.MasterElasticSearchService;

@Slf4j
@Component
public class UpdateConnectionReceiver implements MessageReceiver<ConnectionInfoDto> {

  private final ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper;

  private final MasterElasticSearchService masterElasticSearchService;

  private final MasterElasticSearchRepository masterElasticSearchRepository;

  private final ConnectionInfoMapper connectionInfoMapper;

  /**
   * Class to update ES index from connection manual update.
   */
  public UpdateConnectionReceiver(
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
   * Handles manual add / remove connections.
   *
   * @param doctor new connection info of a trainee
   */
  @Override
  public void handleMessage(ConnectionInfoDto doctor) {
    try {
      List<MasterDoctorView> existingViews = masterElasticSearchRepository
          .findByGmcReferenceNumber(doctor.getGmcReferenceNumber());
      List<ConnectionInfoDto> updatedConnections = updateExistingViews(
          existingViews, doctor.getDesignatedBody(), doctor.getExceptionReason());
      updatedConnections.forEach(connection -> {
        masterElasticSearchService.updateMasterIndex(connection);
        elasticSearchIndexUpdateHelper.updateElasticSearchIndex(connection);
      });
    } catch (Exception e) {
      log.info("Exception in `UpdateConnectionReceiver` (GmcId: {}): {}",
          doctor.getGmcReferenceNumber(),  e);
    }
  }

  private List<ConnectionInfoDto> updateExistingViews(
      List<MasterDoctorView> existingViews,
      String designatedBodyCode,
      String exceptionMessage
  ) {
    List<ConnectionInfoDto> connectionInfoDtos = new ArrayList<>();
    existingViews.forEach(existingView -> {
      if (exceptionMessage.equals(SUCCESS.getMessage())) {
        existingView.setDesignatedBody(designatedBodyCode);
        existingView.setConnectionStatus(StringUtils.hasText(designatedBodyCode) ? "Yes" : "No");
      }
      else {
        existingView.setExceptionReason(exceptionMessage);
      }
      connectionInfoDtos.add(connectionInfoMapper.masterToDto(existingView));
    });
    return connectionInfoDtos;
  }
}
