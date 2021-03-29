package uk.nhs.hee.tis.revalidation.connection.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import uk.nhs.hee.tis.revalidation.connection.entity.DisconnectedView;

@Repository
public interface DisconnectedElasticSearchRepository
    extends ElasticsearchRepository<DisconnectedView, String> {

  void deleteByGmcReferenceNumber(String gmcReferenceNumber);

  void deleteByTcsPersonId(Long tcsPersonId);
}
