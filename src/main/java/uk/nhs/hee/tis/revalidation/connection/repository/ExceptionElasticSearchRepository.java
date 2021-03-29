package uk.nhs.hee.tis.revalidation.connection.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionView;

@Repository
public interface ExceptionElasticSearchRepository extends ElasticsearchRepository<ExceptionView, String> {

  void deleteByGmcReferenceNumber(String gmcReferenceNumber);

  void deleteByTcsPersonId(Long tcsPersonId);
}
