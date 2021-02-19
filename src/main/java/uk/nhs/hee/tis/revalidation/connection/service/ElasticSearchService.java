package uk.nhs.hee.tis.revalidation.connection.service;

import static java.util.stream.Collectors.toList;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionView;
import uk.nhs.hee.tis.revalidation.connection.repository.ExceptionElasticSearchRepository;

@Slf4j
@Service
public class ElasticSearchService {

  @Autowired
  private ExceptionElasticSearchRepository exceptionElasticSearchRepository;

  public void sendToElasticSearch(List<ExceptionView> dataToSave) {
    if (CollectionUtils.isNotEmpty(dataToSave)) {
      List<ExceptionView> dataForES = dataToSave.stream().map(gmcDoctor -> ExceptionView.builder()
          .gmcReferenceNumber(gmcDoctor.getGmcReferenceNumber())
          .doctorFirstName(gmcDoctor.getDoctorFirstName())
          .doctorLastName(gmcDoctor.getDoctorLastName())
          .build()).collect(toList());
      exceptionElasticSearchRepository.saveAll(dataForES);
    }
  }

}
