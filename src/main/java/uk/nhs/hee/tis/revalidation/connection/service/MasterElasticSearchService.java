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

package uk.nhs.hee.tis.revalidation.connection.service;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.util.iterable.Iterables;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchScrollHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.MasterDoctorView;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.MasterElasticSearchRepository;

@Service
@Slf4j
public class MasterElasticSearchService {

  private MasterElasticSearchRepository masterElasticSearchRepository;

  private ConnectionInfoMapper connectionInfoMapper;

  /**
   * constructor.
   */
  public MasterElasticSearchService(
      MasterElasticSearchRepository masterElasticSearchRepository,
      ConnectionInfoMapper connectionInfoMapper) {
    this.masterElasticSearchRepository = masterElasticSearchRepository;
    this.connectionInfoMapper = connectionInfoMapper;
  }

  public void updateMasterIndex(ConnectionInfoDto connectionInfoDto) {
    MasterDoctorView masterDoctorToSave = connectionInfoMapper
        .dtoToMaster(connectionInfoDto);
    try {
      Iterable<MasterDoctorView> existingRecords = findMasterDoctorRecordByGmcNumberPersonId(masterDoctorToSave);
      if(Iterables.size(existingRecords) > 0) {
        updateRecords(existingRecords, masterDoctorToSave);
      } else {
        masterElasticSearchRepository.save(masterDoctorToSave);
      }
     } catch (Exception e) {
      log.info("Exception in `upsertMasterIndex`"
              + "(GmcId: {}; PersonId: {}): {}",
          masterDoctorToSave.getGmcReferenceNumber(),
          masterDoctorToSave.getTcsPersonId(),
          e.getMessage());
    }
  }

  private Iterable<MasterDoctorView> findMasterDoctorRecordByGmcNumberPersonId(
      MasterDoctorView dataToSave) {
    Iterable<MasterDoctorView> result = new ArrayList<>();

    if (dataToSave.getGmcReferenceNumber() != null && dataToSave.getTcsPersonId() != null) {
      try {
        result = masterElasticSearchRepository.findByGmcReferenceNumberAndTcsPersonId(
            dataToSave.getGmcReferenceNumber(),
            dataToSave.getTcsPersonId());
      }
      catch (Exception ex) {
        log.info("Exception in `findByGmcReferenceNumberAndTcsPersonId`"
                + "(GmcId: {}; PersonId: {}): {}",
            dataToSave.getGmcReferenceNumber(),dataToSave.getTcsPersonId(),  ex);
      }
    }

    else if (dataToSave.getGmcReferenceNumber() != null
        && dataToSave.getTcsPersonId() == null) {
      try {
        result = masterElasticSearchRepository.findByGmcReferenceNumber(
            dataToSave.getGmcReferenceNumber());
      }
      catch (Exception ex) {
        log.info("Exception in `findByGmcReferenceNumber` (GmcId: {}): {}",
            dataToSave.getGmcReferenceNumber(),  ex);
      }
    }

    else if (dataToSave.getGmcReferenceNumber() == null
        && dataToSave.getTcsPersonId() != null) {
      try {
        result = masterElasticSearchRepository.findByTcsPersonId(
            dataToSave.getTcsPersonId());
      }
      catch (Exception ex) {
        log.info("Exception in `findByTcsPersonId` (PersonId: {}): {}",
            dataToSave.getTcsPersonId(),  ex);
      }
    }

    return result;
  }

  private void updateRecords(Iterable<MasterDoctorView> existingRecords, MasterDoctorView dataToSave) {
    existingRecords.forEach(currentDoctorView -> {
      dataToSave.setId(currentDoctorView.getId());
      masterElasticSearchRepository.save(dataToSave);
    });
  }
}
