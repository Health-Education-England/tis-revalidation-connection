package uk.nhs.hee.tis.revalidation.connection.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class GmcDoctor implements Serializable {

  private String gmcReferenceNumber;
  private String doctorFirstName;
  private String doctorLastName;
  private String submissionDate;
  private String doctorStatus;
  private String breach;
  private String dateAdded;
  private String underNotice;
  private String investigation;
  private String preliminaryInvestigation;
  private String sanction;
  private String designatedBodyCode;
}
