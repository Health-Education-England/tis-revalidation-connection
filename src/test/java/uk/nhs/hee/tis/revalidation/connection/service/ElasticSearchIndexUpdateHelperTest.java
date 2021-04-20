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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectedView;
import uk.nhs.hee.tis.revalidation.connection.entity.DisconnectedView;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionView;

@ExtendWith(MockitoExtension.class)
public class ElasticSearchIndexUpdateHelperTest {

  private static final String VISITOR = "Visitor";
  private static final String SUBSTANTIVE = "Substantive";
  private static final String CONNECTED = "Yes";
  private static final String DISCONNECTED = "No";
  @Mock
  UpdateExceptionElasticSearchService updateExceptionElasticSearchService;
  @Mock
  UpdateConnectedElasticSearchService updateConnectedElasticSearchService;
  @Mock
  UpdateDisconnectedElasticSearchService updateDisconnectedElasticSearchService;
  @InjectMocks
  ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper;
  private ConnectionInfoDto visitorExceptionDto = ConnectionInfoDto.builder()
      .tcsPersonId((long) 111)
      .gmcReferenceNumber("123")
      .doctorFirstName("first")
      .doctorLastName("last")
      .submissionDate(LocalDate.now())
      .programmeName("programme")
      .programmeMembershipType(VISITOR)
      .designatedBody("body")
      .tcsDesignatedBody("tcsbody")
      .programmeOwner("owner")
      .connectionStatus("Yes")
      .programmeMembershipStartDate(LocalDate.now().minusDays(100))
      .programmeMembershipEndDate(LocalDate.now().plusDays(100))
      .dataSource("source")
      .build();
  private ConnectionInfoDto noConnectionExceptionDto = ConnectionInfoDto.builder()
      .tcsPersonId((long) 111)
      .gmcReferenceNumber("123")
      .doctorFirstName("first")
      .doctorLastName("last")
      .submissionDate(LocalDate.now())
      .programmeName("programme")
      .programmeMembershipType(VISITOR)
      .designatedBody(null)
      .tcsDesignatedBody("tcsbody")
      .programmeOwner("owner")
      .connectionStatus("Yes")
      .programmeMembershipStartDate(LocalDate.now().minusDays(100))
      .programmeMembershipEndDate(LocalDate.now().plusDays(100))
      .dataSource("source")
      .build();
  private ConnectionInfoDto noExceptionDto = ConnectionInfoDto.builder()
      .tcsPersonId((long) 111)
      .gmcReferenceNumber("123")
      .doctorFirstName("first")
      .doctorLastName("last")
      .submissionDate(LocalDate.now())
      .programmeName("programme")
      .programmeMembershipType(SUBSTANTIVE)
      .designatedBody("body")
      .tcsDesignatedBody("tcsbody")
      .programmeOwner("owner")
      .connectionStatus("Yes")
      .programmeMembershipStartDate(LocalDate.now().minusDays(100))
      .programmeMembershipEndDate(LocalDate.now().plusDays(100))
      .dataSource("source")
      .build();

  @Test
  void shouldAddExceptionIfVisitor() {
    elasticSearchIndexUpdateHelper.updateElasticSearchIndex(visitorExceptionDto);
    verify(updateExceptionElasticSearchService).saveExceptionViews(
        elasticSearchIndexUpdateHelper.getExceptionViews(visitorExceptionDto));
    verify(updateConnectedElasticSearchService).saveConnectedViews(
        elasticSearchIndexUpdateHelper.getConnectedViews(visitorExceptionDto));
    verify(updateDisconnectedElasticSearchService).removeDisconnectedViewByGmcNumber(
        visitorExceptionDto.getGmcReferenceNumber());
    verify(updateDisconnectedElasticSearchService).removeDisconnectedViewByTcsPersonId(
        visitorExceptionDto.getTcsPersonId());
  }

  @Test
  void shouldAddExceptionIfProgrammeMembershipExpired() {
    ConnectionInfoDto pmExpiredExceptionDto = noExceptionDto;
    pmExpiredExceptionDto.setProgrammeMembershipStartDate(LocalDate.now().minusDays(200));
    pmExpiredExceptionDto.setProgrammeMembershipEndDate(LocalDate.now().minusDays(100));
    elasticSearchIndexUpdateHelper.updateElasticSearchIndex(pmExpiredExceptionDto);
    verify(updateExceptionElasticSearchService).saveExceptionViews(
        elasticSearchIndexUpdateHelper.getExceptionViews(pmExpiredExceptionDto));
    verify(updateConnectedElasticSearchService).saveConnectedViews(
        elasticSearchIndexUpdateHelper.getConnectedViews(pmExpiredExceptionDto));
    verify(updateDisconnectedElasticSearchService).removeDisconnectedViewByGmcNumber(
        pmExpiredExceptionDto.getGmcReferenceNumber());
    verify(updateDisconnectedElasticSearchService).removeDisconnectedViewByTcsPersonId(
        pmExpiredExceptionDto.getTcsPersonId());
  }

  @Test
  void shouldAddExceptionIfVisitorAndProgrammeMembershipExpired() {
    ConnectionInfoDto visitorPmExpiredExceptionDto = noExceptionDto;
    visitorPmExpiredExceptionDto.setProgrammeMembershipType(VISITOR);
    visitorPmExpiredExceptionDto.setProgrammeMembershipStartDate(LocalDate.now().minusDays(200));
    visitorPmExpiredExceptionDto.setProgrammeMembershipEndDate(LocalDate.now().minusDays(100));
    elasticSearchIndexUpdateHelper.updateElasticSearchIndex(visitorPmExpiredExceptionDto);
    verify(updateExceptionElasticSearchService).saveExceptionViews(
        elasticSearchIndexUpdateHelper.getExceptionViews(visitorPmExpiredExceptionDto));
    verify(updateConnectedElasticSearchService).saveConnectedViews(
        elasticSearchIndexUpdateHelper.getConnectedViews(visitorPmExpiredExceptionDto));
    verify(updateDisconnectedElasticSearchService).removeDisconnectedViewByGmcNumber(
        visitorPmExpiredExceptionDto.getGmcReferenceNumber());
    verify(updateDisconnectedElasticSearchService).removeDisconnectedViewByTcsPersonId(
        visitorPmExpiredExceptionDto.getTcsPersonId());
  }

  @Test
  void shouldNotAddExceptionIfProgrammeMembershipExpiredButDisconnected() {
    ConnectionInfoDto visitorPmExpiredExceptionDto = noExceptionDto;
    visitorPmExpiredExceptionDto.setProgrammeMembershipStartDate(LocalDate.now().minusDays(200));
    visitorPmExpiredExceptionDto.setProgrammeMembershipEndDate(LocalDate.now().minusDays(100));
    visitorPmExpiredExceptionDto.setDesignatedBody(null);
    visitorPmExpiredExceptionDto.setConnectionStatus("No");
    elasticSearchIndexUpdateHelper.updateElasticSearchIndex(visitorPmExpiredExceptionDto);
    verify(updateExceptionElasticSearchService, never()).saveExceptionViews(
        elasticSearchIndexUpdateHelper.getExceptionViews(visitorPmExpiredExceptionDto));
    verify(updateConnectedElasticSearchService, never()).saveConnectedViews(
        elasticSearchIndexUpdateHelper.getConnectedViews(visitorPmExpiredExceptionDto));
    verify(updateExceptionElasticSearchService).removeExceptionViewByGmcNumber(
        visitorPmExpiredExceptionDto.getGmcReferenceNumber());
    verify(updateExceptionElasticSearchService).removeExceptionViewByTcsPersonId(
        visitorPmExpiredExceptionDto.getTcsPersonId());
    verify(updateDisconnectedElasticSearchService, never()).removeDisconnectedViewByTcsPersonId(
        visitorPmExpiredExceptionDto.getTcsPersonId());
  }

  @Test
  void shouldNotThrowErrorIfProgrammeMembershipTypeNull() {
    ConnectionInfoDto visitorPmExpiredExceptionDto = noExceptionDto;
    visitorPmExpiredExceptionDto.setProgrammeMembershipType(null);
    elasticSearchIndexUpdateHelper.updateElasticSearchIndex(visitorPmExpiredExceptionDto);
    verify(updateExceptionElasticSearchService, never()).saveExceptionViews(
        elasticSearchIndexUpdateHelper.getExceptionViews(visitorPmExpiredExceptionDto));
  }

  @Test
  void shouldNotThrowErrorIfProgrammeMembershipEndDateNull() {
    ConnectionInfoDto visitorPmExpiredExceptionDto = noExceptionDto;
    visitorPmExpiredExceptionDto.setProgrammeMembershipEndDate(null);
    elasticSearchIndexUpdateHelper.updateElasticSearchIndex(visitorPmExpiredExceptionDto);
    verify(updateExceptionElasticSearchService, never()).saveExceptionViews(
        elasticSearchIndexUpdateHelper.getExceptionViews(visitorPmExpiredExceptionDto));
  }

  @Test
  void shouldRemoveExceptionIfNotVisitor() {
    elasticSearchIndexUpdateHelper.updateElasticSearchIndex(noExceptionDto);
    verify(updateExceptionElasticSearchService).removeExceptionViewByGmcNumber(
        noExceptionDto.getGmcReferenceNumber()
    );
    verify(updateExceptionElasticSearchService).removeExceptionViewByTcsPersonId(
        noExceptionDto.getTcsPersonId()
    );
  }

  @Test
  void shouldGetExceptionViews() {
    final ExceptionView returnedView = elasticSearchIndexUpdateHelper
        .getExceptionViews(visitorExceptionDto);

    assert (returnedView.getGmcReferenceNumber())
        .equals(visitorExceptionDto.getGmcReferenceNumber());
    assert (returnedView.getDoctorFirstName()).equals(visitorExceptionDto.getDoctorFirstName());
    assert (returnedView.getDoctorLastName()).equals(visitorExceptionDto.getDoctorLastName());
    assert (returnedView.getSubmissionDate()).equals(visitorExceptionDto.getSubmissionDate());
    assert (returnedView.getProgrammeName()).equals(visitorExceptionDto.getProgrammeName());
    assert (returnedView.getMembershipType())
        .equals(visitorExceptionDto.getProgrammeMembershipType());
    assert (returnedView.getDesignatedBody()).equals(visitorExceptionDto.getDesignatedBody());
    assert (returnedView.getTcsDesignatedBody()).equals(visitorExceptionDto.getTcsDesignatedBody());
    assert (returnedView.getProgrammeOwner()).equals(visitorExceptionDto.getProgrammeOwner());
    assert (returnedView.getMembershipStartDate())
        .equals(visitorExceptionDto.getProgrammeMembershipStartDate());
    assert (returnedView.getMembershipEndDate())
        .equals(visitorExceptionDto.getProgrammeMembershipEndDate());
    assertThat(returnedView.getConnectionStatus(), is(CONNECTED));
  }

  @Test
  void shouldGetConnectedViews() {
    final ConnectedView returnedView = elasticSearchIndexUpdateHelper
        .getConnectedViews(visitorExceptionDto);

    assert (returnedView.getGmcReferenceNumber())
        .equals(visitorExceptionDto.getGmcReferenceNumber());
    assert (returnedView.getDoctorFirstName()).equals(visitorExceptionDto.getDoctorFirstName());
    assert (returnedView.getDoctorLastName()).equals(visitorExceptionDto.getDoctorLastName());
    assert (returnedView.getSubmissionDate()).equals(visitorExceptionDto.getSubmissionDate());
    assert (returnedView.getProgrammeName()).equals(visitorExceptionDto.getProgrammeName());
    assert (returnedView.getMembershipType())
        .equals(visitorExceptionDto.getProgrammeMembershipType());
    assert (returnedView.getDesignatedBody()).equals(visitorExceptionDto.getDesignatedBody());
    assert (returnedView.getTcsDesignatedBody()).equals(visitorExceptionDto.getTcsDesignatedBody());
    assert (returnedView.getProgrammeOwner()).equals(visitorExceptionDto.getProgrammeOwner());
    assert (returnedView.getMembershipStartDate())
        .equals(visitorExceptionDto.getProgrammeMembershipStartDate());
    assert (returnedView.getMembershipEndDate())
        .equals(visitorExceptionDto.getProgrammeMembershipEndDate());
    assertThat(returnedView.getConnectionStatus(), is(CONNECTED));
  }

  @Test
  void shouldGetDisconnectedViews() {
    final DisconnectedView returnedView = elasticSearchIndexUpdateHelper
        .getDisconnectedViews(visitorExceptionDto);

    assert (returnedView.getGmcReferenceNumber())
        .equals(visitorExceptionDto.getGmcReferenceNumber());
    assert (returnedView.getDoctorFirstName()).equals(visitorExceptionDto.getDoctorFirstName());
    assert (returnedView.getDoctorLastName()).equals(visitorExceptionDto.getDoctorLastName());
    assert (returnedView.getSubmissionDate()).equals(visitorExceptionDto.getSubmissionDate());
    assert (returnedView.getProgrammeName()).equals(visitorExceptionDto.getProgrammeName());
    assert (returnedView.getMembershipType())
        .equals(visitorExceptionDto.getProgrammeMembershipType());
    assert (returnedView.getDesignatedBody()).equals(visitorExceptionDto.getDesignatedBody());
    assert (returnedView.getTcsDesignatedBody()).equals(visitorExceptionDto.getTcsDesignatedBody());
    assert (returnedView.getProgrammeOwner()).equals(visitorExceptionDto.getProgrammeOwner());
    assert (returnedView.getMembershipStartDate())
        .equals(visitorExceptionDto.getProgrammeMembershipStartDate());
    assert (returnedView.getMembershipEndDate())
        .equals(visitorExceptionDto.getProgrammeMembershipEndDate());
    assertThat(returnedView.getConnectionStatus(), is(CONNECTED));
  }
}
