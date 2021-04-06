package uk.nhs.hee.tis.revalidation.connection.service;

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectedView;
import uk.nhs.hee.tis.revalidation.connection.entity.DisconnectedView;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionView;


@Slf4j
@Component
public class ElasticSearchIndexUpdateHelper {

  private static final String VISITOR = "Visitor";

  @Autowired
  UpdateExceptionElasticSearchService updateExceptionElasticSearchService;

  @Autowired
  UpdateConnectedElasticSearchService updateConnectedElasticSearchService;

  @Autowired
  UpdateDisconnectedElasticSearchService updateDisconnectedElasticSearchService;

  /**
   * Route changes to correct elasticsearch index
   *
   * @param connectionInfo details of changes that need to be propagated to elasticsearch
   */
  public void updateElasticSearchIndex(final ConnectionInfoDto connectionInfo) {
    connectionInfo.setConnectionStatus(getConnectionStatus(connectionInfo.getDesignatedBody()));

    // Check Exception
    checkException(connectionInfo);

    // Check Connection Status
    checkTraineeConnection(connectionInfo);
  }

  private void checkException(final ConnectionInfoDto connectionInfo) {
    if (isException(connectionInfo)) {
      updateExceptionElasticSearchService.saveExceptionViews(getExceptionViews(connectionInfo));
    }
    else {
      updateExceptionElasticSearchService
          .removeExceptionViewByGmcNumber(connectionInfo.getGmcReferenceNumber());
      updateExceptionElasticSearchService
          .removeExceptionViewByTcsPersonId(connectionInfo.getTcsPersonId());
    }
  }

  private void checkTraineeConnection(final ConnectionInfoDto connectionInfo) {
    if (isConnected(connectionInfo)) {
      // Save connected trainee to Connected ES index
      updateConnectedElasticSearchService.saveConnectedViews(getConnectedViews(connectionInfo));

      // Delete connected trainee from Disconnected ES index
      updateDisconnectedElasticSearchService
          .removeDisconnectedViewByGmcNumber(connectionInfo.getGmcReferenceNumber());
      updateDisconnectedElasticSearchService
          .removeDisconnectedViewByTcsPersonId(connectionInfo.getTcsPersonId());
    }
    else {
      // Save disconnected trainee to Disconnected ES index
      updateDisconnectedElasticSearchService
          .saveDisconnectedViews(getDisconnectedViews(connectionInfo));

      // Delete disconnected trainee from Connected ES index
      updateConnectedElasticSearchService
          .removeConnectedViewByGmcNumber(connectionInfo.getGmcReferenceNumber());
      updateConnectedElasticSearchService
          .removeConnectedViewByTcsPersonId(connectionInfo.getTcsPersonId());
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
      .connectionStatus(connectionInfo.getConnectionStatus())
      .membershipStartDate(connectionInfo.getProgrammeMembershipStartDate())
      .membershipEndDate(connectionInfo.getProgrammeMembershipEndDate())
      .build();
  }

  /**
   * Create entry for connected elasticsearch index.
   *
   * @param connectionInfo details of changes that need to be propagated to elasticsearch
   */
  public ConnectedView getConnectedViews(final ConnectionInfoDto connectionInfo) {
    return ConnectedView.builder()
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
        .connectionStatus(connectionInfo.getConnectionStatus())
        .membershipStartDate(connectionInfo.getProgrammeMembershipStartDate())
        .membershipEndDate(connectionInfo.getProgrammeMembershipEndDate())
        .build();
  }

  /**
   * Create entry for disconnected elasticsearch index.
   *
   * @param connectionInfo details of changes that need to be propagated to elasticsearch
   */
  public DisconnectedView getDisconnectedViews(final ConnectionInfoDto connectionInfo) {
    return DisconnectedView.builder()
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
        .connectionStatus(connectionInfo.getConnectionStatus())
        .membershipStartDate(connectionInfo.getProgrammeMembershipStartDate())
        .membershipEndDate(connectionInfo.getProgrammeMembershipEndDate())
        .build();
  }

  private boolean isException(final ConnectionInfoDto connectionInfo) {
    boolean isVisitor = connectionInfo.getProgrammeMembershipType().equalsIgnoreCase(VISITOR);
    boolean isExpired = connectionInfo.getProgrammeMembershipEndDate().isBefore(LocalDate.now())
        && connectionInfo.getConnectionStatus().equalsIgnoreCase("Yes");

    return (isVisitor || isExpired);
  }

  private boolean isConnected(final ConnectionInfoDto connectionInfo) {
    return connectionInfo.getConnectionStatus().equalsIgnoreCase("Yes");
  }

  private String getConnectionStatus(final String designatedBody) {
    return (designatedBody == null || designatedBody.equals("")) ? "No" : "Yes";
  }

}
