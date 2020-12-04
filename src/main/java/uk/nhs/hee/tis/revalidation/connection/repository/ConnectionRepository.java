package uk.nhs.hee.tis.revalidation.connection.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestLog;

@Repository
public interface ConnectionRepository extends MongoRepository<ConnectionRequestLog, String> {

  List<ConnectionRequestLog> findAllByGmcId(final String gmcId);
}
