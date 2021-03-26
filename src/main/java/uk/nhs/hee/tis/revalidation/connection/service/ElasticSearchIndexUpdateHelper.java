package uk.nhs.hee.tis.revalidation.connection.service;

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionView;


@Slf4j
@Component
public class ElasticSearchIndexUpdateHelper {

  private static final String VISITOR = "Visitor";

  @Autowired
  ElasticSearchService elasticSearchService;

  /**
   * Route changes to correct elasticsearch index
   *
   * @param connectionInfo details of changes that need to be propagated to elasticsearch
   */
  public void updateElasticSearchIndex(final ConnectionInfoDto connectionInfo) {
    if (isException(connectionInfo)) {
      elasticSearchService.saveExceptionViews(getExceptionViews(connectionInfo));
    }
    else {
      // If the trainee does not have a gmc number
      if (connectionInfo.getGmcReferenceNumber() != null) {
        elasticSearchService.removeExceptionViewByGmcNumber(connectionInfo.getGmcReferenceNumber());
      }

      // If the trainee does not have a person Id from TCS
      if (connectionInfo.getTcsPersonId() != null) {
        elasticSearchService.removeExceptionViewByTcsPersonId(connectionInfo.getTcsPersonId());
      }
    }
  }

  /**
   * Create entry for exception elasticsearch index
   *
   * @param connectionInfo details of changes that need to be propagated to elasticsearch
   */
  public ExceptionView getExceptionViews(final ConnectionInfoDto connectionInfo) {
    return ExceptionView.builder()
      .tcsPersonId(connectionInfo.getTcsPersonId())
      .gmcReferenceNumber(connectionInfo.getGmcReferenceNumber())
      .doctorFirstName(connectionInfo.getDoctorFirstName())
      .doctorLastName(connectionInfo.getDoctorLastName())
      .submissionDate(connectionInfo.getSubmissionDate())
      .programmeName(connectionInfo.getProgrammeName())
      .membershipType(connectionInfo.getProgrammeMembershipType())
      .designatedBody(connectionInfo.getDesignatedBody())
      .tcsDesignatedBody(connectionInfo.getTcsDesignatedBody())
      .programmeOwner(connectionInfo.getProgrammeOwner())
      .connectionStatus(getConnectionStatus(connectionInfo.getDesignatedBody()))
      .membershipStartDate(connectionInfo.getProgrammeMembershipStartDate())
      .membershipEndDate(connectionInfo.getProgrammeMembershipEndDate())
      .build();
  }

  private boolean isException(final ConnectionInfoDto connectionInfo) {
    boolean isVisitor = connectionInfo.getProgrammeMembershipType().equalsIgnoreCase(VISITOR);
    boolean isExpired = connectionInfo.getProgrammeMembershipEndDate().isBefore(LocalDate.now());

    if(isVisitor || isExpired) {
      return true;
    }
    return false;
  }

  private String getConnectionStatus(final String designatedBody) {
    return (designatedBody == null || designatedBody.equals("")) ? "No" : "Yes";
  }

}
