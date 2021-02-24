package uk.nhs.hee.tis.revalidation.connection.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(indexName = "exceptionViewIndex")
public class ExceptionView {
  @Id
  private String gmcReferenceNumber;
  private String doctorFirstName;
  private String doctorLastName;
  private LocalDate submissionDate;
  private String programmeName;
  private String programmeMembershipType;
  private String designatedBody;
  private String tcsDesignatedBody;
  private String programmeOwner;
  private String connectionStatus;
  private LocalDate programmeMembershipStartDate;
  private LocalDate programmeMembershipEndDate;
  private String dataSource;
}
