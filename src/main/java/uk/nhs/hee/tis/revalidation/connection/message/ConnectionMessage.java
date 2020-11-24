package uk.nhs.hee.tis.revalidation.connection.message;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConnectionMessage {

  private String gmcId;
  private String designatedBodyCode;
}
