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

  /**
   * add new exceptions to elasticsearch index
   *
   * @param dataToSave exceptions to go in elasticsearch
   */
  public void addExceptionViews(List<ExceptionView> dataToSave) {
    if (CollectionUtils.isNotEmpty(dataToSave)) {
      exceptionElasticSearchRepository.saveAll(dataToSave);
    }
  }

  /**
   * remove exceptions from elasticsearch index
   *
   * @param gmcReferenceNumber id of exception to remove
   */
  public void removeExceptionView(String gmcReferenceNumber) {
    exceptionElasticSearchRepository.deleteById(gmcReferenceNumber);
  }
}
