package uk.nhs.hee.tis.revalidation.connection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A DTO class for displaying details of a hidden discrepancy.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HiddenDiscrepancyDto {
  private String id;
  private String gmcId;
  private String hiddenForDesignatedBodyCode;
  private String hiddenBy;
  private String reason;
  private LocalDateTime hiddenDateTime;
}
