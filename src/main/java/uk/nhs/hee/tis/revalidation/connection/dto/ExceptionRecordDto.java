package uk.nhs.hee.tis.revalidation.connection.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExceptionRecordDto {

  private String gmcId;
  private String exceptionMessage;
}
