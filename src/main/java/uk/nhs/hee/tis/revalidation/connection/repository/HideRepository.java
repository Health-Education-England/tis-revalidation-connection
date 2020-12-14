package uk.nhs.hee.tis.revalidation.connection.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.nhs.hee.tis.revalidation.connection.entity.HideConnectionLog;

@Repository
public interface HideRepository extends MongoRepository<HideConnectionLog, String> {

}
