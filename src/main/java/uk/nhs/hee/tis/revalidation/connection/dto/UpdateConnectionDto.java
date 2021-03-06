package uk.nhs.hee.tis.revalidation.connection.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateConnectionDto {

  private String changeReason;
  private String designatedBodyCode;
  private List<DoctorInfoDto> doctors;
}
