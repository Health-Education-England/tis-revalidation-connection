package uk.nhs.hee.tis.revalidation.connection.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "connectionLogs")
public class ConnectionRequestLog {

  @Id
  private String id;
  private String gmcId;
  private String gmcClientId;
  private String reason;
  private ConnectionRequestType requestType;
  private LocalDateTime requestTime;
  private String responseCode;
}
