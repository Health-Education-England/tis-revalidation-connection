/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.revalidation.connection.service;

import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.IndexNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
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
  ExceptionElasticSearchService exceptionElasticSearchService;

  @Autowired
  ConnectedElasticSearchService connectedElasticSearchService;

  @Autowired
  DisconnectedElasticSearchService disconnectedElasticSearchService;

  @Autowired
  private ElasticsearchOperations elasticSearchOperations;

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
      exceptionElasticSearchService.saveExceptionViews(getExceptionViews(connectionInfo));
    } else {
      exceptionElasticSearchService
          .removeExceptionViewByGmcNumber(connectionInfo.getGmcReferenceNumber());
      exceptionElasticSearchService
          .removeExceptionViewByTcsPersonId(connectionInfo.getTcsPersonId());
    }
  }

  private void checkTraineeConnection(final ConnectionInfoDto connectionInfo) {
    if (isConnected(connectionInfo)) {
      // Save connected trainee to Connected ES index
      connectedElasticSearchService.saveConnectedViews(getConnectedViews(connectionInfo));

      // Delete connected trainee from Disconnected ES index
      disconnectedElasticSearchService
          .removeDisconnectedViewByGmcNumber(connectionInfo.getGmcReferenceNumber());
      disconnectedElasticSearchService
          .removeDisconnectedViewByTcsPersonId(connectionInfo.getTcsPersonId());
    } else {
      // Save disconnected trainee to Disconnected ES index
      disconnectedElasticSearchService
          .saveDisconnectedViews(getDisconnectedViews(connectionInfo));

      // Delete disconnected trainee from Connected ES index
      connectedElasticSearchService
          .removeConnectedViewByGmcNumber(connectionInfo.getGmcReferenceNumber());
      connectedElasticSearchService
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
    var isVisitor = false;
    if (connectionInfo.getProgrammeMembershipType() != null) {
      isVisitor = connectionInfo.getProgrammeMembershipType().equalsIgnoreCase(VISITOR);
    }
    var isExpired = false;
    if (connectionInfo.getProgrammeMembershipEndDate() != null) {
      isExpired = (connectionInfo.getProgrammeMembershipEndDate().isBefore(LocalDate.now())
          && connectionInfo.getConnectionStatus().equalsIgnoreCase("Yes"));
    }
    return (isVisitor || isExpired);
  }

  private boolean isConnected(final ConnectionInfoDto connectionInfo) {
    return connectionInfo.getConnectionStatus().equalsIgnoreCase("Yes");
  }

  private String getConnectionStatus(final String designatedBody) {
    return (designatedBody == null || designatedBody.equals("")) ? "No" : "Yes";
  }

  public void clearConnectionIndexes(List<String> connectionIndices) {
    connectionIndices.forEach(conIndex -> deleteConnectionIndex(conIndex));
    connectionIndices.forEach(conIndex -> createConnectionIndex(conIndex));
  }

  private void deleteConnectionIndex(String esIndex) {
    log.info("Deleting elastic search index: {}", esIndex);
    try {
      elasticSearchOperations.deleteIndex(esIndex);
    } catch (IndexNotFoundException e) {
      log.info("Could not delete an index that does not exist: {}, continuing", esIndex);
    }
  }

  private void createConnectionIndex(String esIndex) {
    log.info("Creating elastic search index: {}", esIndex);
    elasticSearchOperations.createIndex(esIndex);
    switch (esIndex) {
      case "connectedindex":
        elasticSearchOperations.putMapping(ConnectedView.class);
        break;
      case "disconnectedindex":
        elasticSearchOperations.putMapping(DisconnectedView.class);
        break;
      case "exceptionindex":
        elasticSearchOperations.putMapping(ExceptionView.class);
        break;
    }
  }
}
