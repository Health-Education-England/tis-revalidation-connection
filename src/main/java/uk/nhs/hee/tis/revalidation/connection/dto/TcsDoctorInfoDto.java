package uk.nhs.hee.tis.revalidation.connection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A DTO to hold the programme information of a doctor.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TcsDoctorInfoDto {
  private String gmcReferenceNumber;
  private String tcsDesignatedBody;
  private String programmeOwner;
  private String dataSource;
}
