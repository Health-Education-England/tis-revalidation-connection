package uk.nhs.hee.tis.revalidation.connection.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ConnectionInfoDto {

  String gmcReferenceNumber;
  String doctorFirstName;
  String doctorLastName;
  LocalDate submissionDate;
  String programmeName;
  String programmeMembershipType;
  String designatedBody;
  String tcsDesignatedBody;
  String programmeOwner;
  String connectionStatus;
  LocalDate programmeMembershipStartDate;
  LocalDate programmeMembershipEndDate;
  String dataSource;
}
