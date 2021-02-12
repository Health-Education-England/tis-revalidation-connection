package uk.nhs.hee.tis.revalidation.connection.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import org.springframework.stereotype.Repository;
import uk.nhs.hee.tis.revalidation.connection.entity.PersonView;

@Repository
public interface PersonSearchRespository extends ElasticsearchRepository<PersonView, String> {
}
