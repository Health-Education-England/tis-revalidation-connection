package uk.nhs.hee.tis.revalidation.connection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

/**
 * A DTO to hold the programme information of a doctor.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TcsDoctorInfoDto {
  private Long tcsPersonId;
  private String gmcReferenceNumber;
  private String doctorFirstName;
  private String doctorLastName;
  private LocalDate submissionDate;
  private String programmeName;
  private String programmeMembershipType;
  private String designatedBody;
  private String tcsDesignatedBody;
  private String programmeOwner;
  private String placementGrade;
  private LocalDate programmeMembershipStartDate;
  private LocalDate programmeMembershipEndDate;
  private LocalDate curriculumEndDate;
  private String dataSource;
}
