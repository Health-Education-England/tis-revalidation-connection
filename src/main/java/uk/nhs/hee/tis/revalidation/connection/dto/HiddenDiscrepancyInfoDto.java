package uk.nhs.hee.tis.revalidation.connection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.nhs.hee.tis.revalidation.connection.entity.HiddenDiscrepancy;

/**
 * A DTO class for displaying a list of details of hidden discrepancies.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HiddenDiscrepancyInfoDto {

  private String gmcReferenceNumber;
  private String doctorFirstName;
  private String doctorLastName;
  private String programmeName;
  private String designatedBody;
  private String tcsDesignatedBody;
  private List<HiddenDiscrepancy> hiddenDiscrepancies;
}
