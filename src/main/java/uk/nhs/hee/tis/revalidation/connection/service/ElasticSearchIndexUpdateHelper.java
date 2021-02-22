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
      elasticSearchService.sendToElasticSearch(getExceptions(connectionInfo));
      //ESService.updateTrainee(gmcDoctor);
    }
    else {
      log.info("Route to other ES index ...");
    }
  }

  private boolean isException(final ConnectionInfoDto connectionInfo) {
    /*  BUSINESS LOGIC WILL GO HERE */
    boolean isVisitor = connectionInfo.getProgrammeMembershipType().equalsIgnoreCase(VISITOR);
    boolean isExpired = connectionInfo.getProgrammeMembershipEndDate().isAfter(LocalDate.now());

    if(isVisitor || isExpired) {
      return true;
    }
    return false;
  }

  private List<ExceptionView> getExceptions(final ConnectionInfoDto connectionInfo) {
    List<ExceptionView> exceptions = new ArrayList<ExceptionView>();
    exceptions.add(
        ExceptionView.builder()
            .gmcReferenceNumber(connectionInfo.getGmcReferenceNumber())
            .doctorFirstName(connectionInfo.getDoctorFirstName())
            .doctorLastName(connectionInfo.getDoctorLastName())
        .build()
    );
    return exceptions;
  }

}
