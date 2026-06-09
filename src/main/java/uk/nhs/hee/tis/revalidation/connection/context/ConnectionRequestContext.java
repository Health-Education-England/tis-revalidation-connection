package uk.nhs.hee.tis.revalidation.connection.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.nhs.hee.tis.revalidation.connection.dto.DoctorInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestType;

@Data
@AllArgsConstructor
@Builder
public class ConnectionRequestContext {

  private String changeReason;
  private String designatedBodyCode;
  private DoctorInfoDto doctor;
  private ConnectionRequestType connectionRequestType;
  private String admin;
}
