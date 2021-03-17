package uk.nhs.hee.tis.revalidation.connection.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExceptionSummaryDto {

  private long countTotal;
  private long totalPages;
  private long totalResults;
  private List<ConnectionInfoDto> connections;
}
