package uk.nhs.hee.tis.revalidation.connection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mongodb.lang.Nullable;
import io.swagger.annotations.ApiModel;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(description = "Connection change details")
public class ConnectionLogDto {
  private String gmcId;
  private String newDesignatedBodyCode;
  private String previousDesignatedBodyCode;
  private String updatedBy;
  private LocalDateTime eventDateTime;
}
