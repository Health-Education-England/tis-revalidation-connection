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

package uk.nhs.hee.tis.revalidation.connection.message;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.util.iterable.Iterables;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.MasterDoctorView;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.MasterElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.connection.service.ConnectionService;
import uk.nhs.hee.tis.revalidation.connection.service.ElasticSearchIndexUpdateHelper;
import uk.nhs.hee.tis.revalidation.connection.service.UpdateExceptionElasticSearchService;


@Slf4j
@Component
public class RabbitMessageListener {

  @Autowired
  private UpdateExceptionElasticSearchService updateExceptionElasticSearchService;

  @Autowired
  private ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper;

  @Autowired
  private ConnectionService connectionService;

  @Autowired
  private MasterElasticSearchRepository masterElasticSearchRepository ;

  @Autowired
  private ConnectionInfoMapper connectionInfoMapper;

  /**
   * handle update event.
   *
   * @param connectionInfo connection information of the trainee
   */
  @RabbitListener(queues = "${app.rabbit.reval.queue.connection.update}")
  public void receiveMessage(final ConnectionInfoDto connectionInfo) {
    log.info("MESSAGE RECEIVED: " + connectionInfo);

    // Get Gmc data and aggregate it to connectionInfo
    if (connectionInfo.getGmcReferenceNumber() != null) {
      Optional<MasterDoctorView> optionalGmcData = masterElasticSearchRepository
          .findFirstByGmcReferenceNumberOrderBySubmissionDateDesc(connectionInfo.getGmcReferenceNumber());
      if (optionalGmcData.isPresent()) {
        final MasterDoctorView masterData = optionalGmcData.get();
        connectionInfo.setSubmissionDate(masterData.getSubmissionDate());
        connectionInfo.setDesignatedBody(masterData.getDesignatedBody());
      }
    }
    elasticSearchIndexUpdateHelper.updateElasticSearchIndex(connectionInfo);
  }

  /**
   * get trainee from Master index then update connection indexes.
   */
  @RabbitListener(queues = "${app.rabbit.reval.queue.connection.getmaster}")
  public void receiveMessage() {
    final Iterable<MasterDoctorView> masterList = masterElasticSearchRepository.findAll();
    log.info("Found {} records from ES Master index: ", Iterables.size(masterList));
    connectionInfoMapper.masterToDtos(masterList).forEach(connectionInfo ->
        elasticSearchIndexUpdateHelper.updateElasticSearchIndex(connectionInfo));
  }

}
