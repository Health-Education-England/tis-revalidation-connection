package uk.nhs.hee.tis.revalidation.connection.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
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
  String dataSource;
  LocalDate programmeMembershipStartDate;
  LocalDate programmeMembershipEndDate;
}
