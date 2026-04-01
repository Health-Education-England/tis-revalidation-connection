package uk.nhs.hee.tis.revalidation.connection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A DTO class for displaying a list of details of hidden discrepancies and pagination info.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HiddenDiscrepancySummaryDto {

  private long countTotal;
  private long totalPages;
  private long totalResults;
  List<HiddenDiscrepancyInfoDto> hiddenDiscrepancies;
}
