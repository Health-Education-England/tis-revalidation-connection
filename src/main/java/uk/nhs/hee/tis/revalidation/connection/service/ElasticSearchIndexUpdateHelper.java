package uk.nhs.hee.tis.revalidation.connection.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ElasticSearchIndexUpdateHelper {

  private static final String VISITOR = "Visitor";

  @Autowired
  ElasticSearchService elasticSearchService;

  public void updateElasticSearchIndex(final ConnectionInfoDto connectionInfo) {
    if(isException(connectionInfo)) {
      elasticSearchService.addExceptionViews(getExceptionViews(connectionInfo));
      //ESService.updateTrainee(gmcDoctor);
    }
    else {
      //NEED TO CHECK IF FAILURE BEFORE REMOVING
      elasticSearchService.removeExceptionView(connectionInfo.getGmcReferenceNumber());
    }
  }

  public List<ExceptionView> getExceptionViews(final ConnectionInfoDto connectionInfo) {
    // TODO 2. get missing fields from gmc/mongodb
    List<ExceptionView> exceptions = new ArrayList<ExceptionView>();
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
//            .programmeMembershipStartDate(connectionInfo.getProgrammeMembershipStartDate())
//            .programmeMembershipEndDate(connectionInfo.getProgrammeMembershipEndDate())
            .build()
    );
    return exceptions;
  }

  private boolean isException(final ConnectionInfoDto connectionInfo) {
    boolean isVisitor = connectionInfo.getProgrammeMembershipType().equalsIgnoreCase(VISITOR);
//    boolean isExpired = connectionInfo.getProgrammeMembershipEndDate().isBefore(LocalDate.now());
    boolean isExpired = false;
     //NEED TO CHECK IF FAILURE BEFORE REMOVING

    if(isVisitor || isExpired) {
      return true;
    }
    return false;
  }

  private String getConnectionStatus(final String designatedBody) {
    return (designatedBody == null || designatedBody.equals("")) ? "No" : "Yes";
  }

}
