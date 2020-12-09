package uk.nhs.hee.tis.revalidation.connection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(description = "Connection information")
public class ConnectionHistoryDto {

  private String connectionId;
  private String gmcId;
  private String gmcClientId;
  private String newDesignatedBodyCode;
  private String previousDesignatedBodyCode;
  private String reason;
  private String reasonMessage;
  private ConnectionRequestType requestType;
  private LocalDateTime requestTime;
  private String responseCode;
  private String responseMessage;
}
