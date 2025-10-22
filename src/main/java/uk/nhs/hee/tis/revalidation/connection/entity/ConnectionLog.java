package uk.nhs.hee.tis.revalidation.connection.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Document(collection = "connectionLogs")
public class ConnectionLog {

  @Id
  private String id; // this is a UUID converted to a String TODO refactor to UUID
  private String gmcId;
  private String newDesignatedBodyCode;
  private String previousDesignatedBodyCode;
  private String updatedBy;
  private LocalDateTime requestTime; // Maps to eventDateTime in Dto TODO migrate field name TIS21-7884
}
