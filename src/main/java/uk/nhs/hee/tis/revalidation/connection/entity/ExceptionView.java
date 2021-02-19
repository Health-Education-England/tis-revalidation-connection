package uk.nhs.hee.tis.revalidation.connection.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(indexName = "exceptionindex")
public class ExceptionView {

  @Id
  private String gmcReferenceNumber;
  private String doctorFirstName;
  private String doctorLastName;

}
