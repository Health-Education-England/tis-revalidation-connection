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
@Document(collection = "exceptionLogs")
public class ExceptionLog {

  @Id
  private String gmcId;
  private String errorMessage;
  private LocalDateTime timestamp;
}
