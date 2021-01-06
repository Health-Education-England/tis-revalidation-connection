package uk.nhs.hee.tis.revalidation.connection.dto;

import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@ApiModel(description = "Request for trainee data for exception page")
public class ExceptionRequestDto {

  private String sortColumn;
  private String sortOrder;
  private int pageNumber;
  private List<String> dbcs;
  private String searchQuery;
}
