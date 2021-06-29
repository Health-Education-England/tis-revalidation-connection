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

package uk.nhs.hee.tis.revalidation.connection.repository.index;

import java.util.List;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import uk.nhs.hee.tis.revalidation.connection.entity.MasterDoctorView;
import uk.nhs.hee.tis.revalidation.connection.repository.index.elasticsearch.ElasticSearchMasterIndexRepository;

/*
Developer note: This interface duplicates IndexRepository as MasterDoctorView
is likely to diverge from other connections views in future
 */
@Repository
public class MasterIndexRepository {
  private ElasticSearchMasterIndexRepository elasticSearchMasterIndexRepository;

  public MasterIndexRepository(
      ElasticSearchMasterIndexRepository elasticSearchMasterIndexRepository
  ) {
    this.elasticSearchMasterIndexRepository = elasticSearchMasterIndexRepository;
  }

  public Page<MasterDoctorView> executePagedQuery(BoolQueryBuilder fullQuery, Pageable pageable) {
    return elasticSearchMasterIndexRepository.search(fullQuery, pageable);
  }

  public List<MasterDoctorView> findViewByGmcReferenceNumberAndTcsPersonId(
      String gmcReferenceNumber,
      Long tcsPersonId
  ) {
    return elasticSearchMasterIndexRepository
        .findViewByGmcReferenceNumberAndTcsPersonId(gmcReferenceNumber,tcsPersonId);
  }

  public List<MasterDoctorView> findViewByGmcReferenceNumber(String gmcReferenceNumber) {
    return elasticSearchMasterIndexRepository.findViewByGmcReferenceNumber(gmcReferenceNumber);
  }

  public List<MasterDoctorView> findViewByTcsPersonId(Long tcsPersonId) {
    return elasticSearchMasterIndexRepository.findViewByTcsPersonId(tcsPersonId);
  }

  public void deleteViewByGmcReferenceNumber(String gmcReferenceNumber) {
    elasticSearchMasterIndexRepository.deleteViewByGmcReferenceNumber(gmcReferenceNumber);
  }

  public void deleteViewByTcsPersonId(Long tcsPersonId) {
    elasticSearchMasterIndexRepository.deleteViewByTcsPersonId(tcsPersonId);
  }

  public void save(MasterDoctorView masterDoctorView) {
    elasticSearchMasterIndexRepository.save(masterDoctorView);
  }
}
