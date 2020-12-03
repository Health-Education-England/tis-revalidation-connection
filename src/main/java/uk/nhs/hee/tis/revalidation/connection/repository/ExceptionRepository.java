package uk.nhs.hee.tis.revalidation.connection.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionLog;

@Repository
public interface ExceptionRepository extends MongoRepository<ExceptionLog, String> {
}
