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

package uk.nhs.hee.tis.revalidation.connection.service.hydration;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.GmcDoctor;
import uk.nhs.hee.tis.revalidation.connection.entity.MasterDoctorView;
import uk.nhs.hee.tis.revalidation.connection.mapper.MasterConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.index.MasterIndexRepository;

@Service
public class ConnectionInfoHydrationService {
  private MasterConnectionInfoMapper masterConnectionInfoMapper;

  private MasterIndexRepository masterIndexRepository;


  public ConnectionInfoHydrationService(
      MasterIndexRepository masterIndexRepository,
      MasterConnectionInfoMapper masterConnectionInfoMapper
  ) {
    this.masterIndexRepository = masterIndexRepository;
    this.masterConnectionInfoMapper = masterConnectionInfoMapper;
  }

  /**
   * Populate missing fields when message comes from TIS
   *
   * @param connectionUpdate details of changes that need to be propagated to index
   */
  public ConnectionInfoDto hydrate(ConnectionInfoDto connectionUpdate) {
      if (connectionUpdate.getGmcReferenceNumber() != null) {
        List<MasterDoctorView> gmcData = masterIndexRepository
            .findViewByGmcReferenceNumber(connectionUpdate.getGmcReferenceNumber());
          if (!gmcData.isEmpty()) {
            MasterDoctorView gmcDataMasterDoctor = gmcData.get(0);
            connectionUpdate.setSubmissionDate(gmcDataMasterDoctor.getSubmissionDate());
            connectionUpdate.setDesignatedBody(gmcDataMasterDoctor.getDesignatedBody());
          }
      }
      return connectionUpdate;
  }

  /**
   * Populate missing fields when message comes from GMC
   *
   * @param doctor details of changes that need to be propagated to index
   */
  public ConnectionInfoDto hydrate(GmcDoctor doctor) {
    MasterDoctorView existingView = masterIndexRepository
        .findViewByGmcReferenceNumber(doctor.getGmcReferenceNumber()).get(0);
    existingView.setDoctorFirstName(doctor.getDoctorFirstName());
    existingView.setDoctorLastName(doctor.getDoctorLastName());
    existingView.setDesignatedBody(doctor.getDesignatedBodyCode());
    return masterConnectionInfoMapper.masterToDto(existingView);
  }
}
