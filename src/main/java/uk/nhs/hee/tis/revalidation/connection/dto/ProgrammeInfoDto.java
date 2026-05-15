package uk.nhs.hee.tis.revalidation.connection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
public class ProgrammeInfoDto {

  Long tcsPersonId;
  String gmcReferenceNumber;
  String doctorFirstName;
  String doctorLastName;
  @JsonDeserialize(using = LocalDateDeserializer.class)
  @JsonSerialize(using = LocalDateSerializer.class)
  LocalDate submissionDate;
  String programmeName;
  String programmeMembershipType;
  String designatedBody;
  String tcsDesignatedBody;
  String programmeOwner;
  String connectionStatus;
  String dataSource;
  @JsonDeserialize(using = LocalDateDeserializer.class)
  @JsonSerialize(using = LocalDateSerializer.class)
  LocalDate programmeMembershipStartDate;
  @JsonDeserialize(using = LocalDateDeserializer.class)
  @JsonSerialize(using = LocalDateSerializer.class)
  LocalDate programmeMembershipEndDate;
  String exceptionReason;
  @JsonDeserialize(using = LocalDateDeserializer.class)
  @JsonSerialize(using = LocalDateSerializer.class)
  LocalDate curriculumEndDate;

  String placementGrade;
  @Nullable
  Boolean syncEnd;
  String updatedBy;
  LocalDateTime lastConnectionDateTime;
  private Boolean notes;
}
