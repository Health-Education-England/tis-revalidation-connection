package uk.nhs.hee.tis.revalidation.connection.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
      elasticSearchService.addExceptionViews(getExceptionViews(connectionInfo));
    }
    else {
      //NEED TO CHECK IF FAILURE BEFORE REMOVING
      elasticSearchService.removeExceptionView(connectionInfo.getGmcReferenceNumber());
    }
  }

  /**
   * Create entry for exception elasticsearch index
   *
   * @param connectionInfo details of changes that need to be propagated to elasticsearch
   */
  public List<ExceptionView> getExceptionViews(final ConnectionInfoDto connectionInfo) {
    List<ExceptionView> exceptions = new ArrayList<>();
    exceptions.add(
        ExceptionView.builder()
            .gmcReferenceNumber(connectionInfo.getGmcReferenceNumber())
            .doctorFirstName(connectionInfo.getDoctorFirstName())
            .doctorLastName(connectionInfo.getDoctorLastName())
            .submissionDate(connectionInfo.getSubmissionDate())
            .programmeName(connectionInfo.getProgrammeName())
            .programmeMembershipType(connectionInfo.getProgrammeMembershipType())
            .designatedBody(connectionInfo.getDesignatedBody())
            .tcsDesignatedBody(connectionInfo.getTcsDesignatedBody())
            .programmeOwner(connectionInfo.getProgrammeOwner())
            .connectionStatus(getConnectionStatus(connectionInfo.getDesignatedBody()))
            .programmeMembershipStartDate(connectionInfo.getProgrammeMembershipStartDate())
            .programmeMembershipEndDate(connectionInfo.getProgrammeMembershipEndDate())
            .build()
    );
    return exceptions;
  }

  private boolean isException(final ConnectionInfoDto connectionInfo) {
    boolean isVisitor = connectionInfo.getProgrammeMembershipType().equalsIgnoreCase(VISITOR);
    boolean isExpired = connectionInfo.getProgrammeMembershipEndDate().isBefore(LocalDate.now());
    //NEED TO CHECK IF FAILURE
    return isVisitor || isExpired;
  }

  private String getConnectionStatus(final String designatedBody) {
    return (designatedBody == null || designatedBody.equals("")) ? "No" : "Yes";
  }

}
