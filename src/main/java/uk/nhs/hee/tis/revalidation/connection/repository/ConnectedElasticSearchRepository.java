package uk.nhs.hee.tis.revalidation.connection.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectedView;

@Repository
public interface ConnectedElasticSearchRepository
    extends ElasticsearchRepository<ConnectedView, String> {

  void deleteByGmcReferenceNumber(String gmcReferenceNumber);

  void deleteByTcsPersonId(Long tcsPersonId);
}
