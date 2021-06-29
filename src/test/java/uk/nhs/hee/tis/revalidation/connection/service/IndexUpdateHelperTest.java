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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectedView;
import uk.nhs.hee.tis.revalidation.connection.entity.DisconnectedView;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionView;
import uk.nhs.hee.tis.revalidation.connection.service.helper.IndexUpdateHelper;
import uk.nhs.hee.tis.revalidation.connection.service.index.IndexServiceImpl;

@ExtendWith(MockitoExtension.class)
class IndexUpdateHelperTest {

  private static final String VISITOR = "Visitor";
  private static final String SUBSTANTIVE = "Substantive";
  private static final String CONNECTED = "Yes";
  private static final String DISCONNECTED = "No";
  private static final List<String> ES_INDICES = List
      .of("connectedindex", "disconnectedindex", "exceptionindex");
  @Mock
  IndexServiceImpl<ExceptionView> exceptionElasticSearchService;
  @Mock
  IndexServiceImpl<ConnectedView> connectedElasticSearchService;
  @Mock
  IndexServiceImpl<DisconnectedView> disconnectedElasticSearchService;
  @Mock
  private ElasticsearchOperations elasticSearchOperations;
  @InjectMocks
  private IndexUpdateHelper indexUpdateHelper;
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
    indexUpdateHelper.updateElasticSearchIndex(visitorExceptionDto);
    verify(exceptionElasticSearchService).saveView(
        indexUpdateHelper.getExceptionViews(visitorExceptionDto));
    verify(connectedElasticSearchService).saveView(
        indexUpdateHelper.getConnectedViews(visitorExceptionDto));
    verify(disconnectedElasticSearchService).removeViewByGmcReferenceNumber(
        visitorExceptionDto.getGmcReferenceNumber());
    verify(disconnectedElasticSearchService).removeViewByTcsPersonId(
        visitorExceptionDto.getTcsPersonId());
  }

  @Test
  void shouldAddExceptionIfProgrammeMembershipExpired() {
    ConnectionInfoDto pmExpiredExceptionDto = noExceptionDto;
    pmExpiredExceptionDto.setProgrammeMembershipStartDate(LocalDate.now().minusDays(200));
    pmExpiredExceptionDto.setProgrammeMembershipEndDate(LocalDate.now().minusDays(100));
    indexUpdateHelper.updateElasticSearchIndex(pmExpiredExceptionDto);
    verify(exceptionElasticSearchService).saveView(
        indexUpdateHelper.getExceptionViews(pmExpiredExceptionDto));
    verify(connectedElasticSearchService).saveView(
        indexUpdateHelper.getConnectedViews(pmExpiredExceptionDto));
    verify(disconnectedElasticSearchService).removeViewByGmcReferenceNumber(
        pmExpiredExceptionDto.getGmcReferenceNumber());
    verify(disconnectedElasticSearchService).removeViewByTcsPersonId(
        pmExpiredExceptionDto.getTcsPersonId());
  }

  @Test
  void shouldAddExceptionIfVisitorAndProgrammeMembershipExpired() {
    ConnectionInfoDto visitorPmExpiredExceptionDto = noExceptionDto;
    visitorPmExpiredExceptionDto.setProgrammeMembershipType(VISITOR);
    visitorPmExpiredExceptionDto.setProgrammeMembershipStartDate(LocalDate.now().minusDays(200));
    visitorPmExpiredExceptionDto.setProgrammeMembershipEndDate(LocalDate.now().minusDays(100));
    indexUpdateHelper.updateElasticSearchIndex(visitorPmExpiredExceptionDto);
    verify(exceptionElasticSearchService).saveView(
        indexUpdateHelper.getExceptionViews(visitorPmExpiredExceptionDto));
    verify(connectedElasticSearchService).saveView(
        indexUpdateHelper.getConnectedViews(visitorPmExpiredExceptionDto));
    verify(disconnectedElasticSearchService).removeViewByGmcReferenceNumber(
        visitorPmExpiredExceptionDto.getGmcReferenceNumber());
    verify(disconnectedElasticSearchService).removeViewByTcsPersonId(
        visitorPmExpiredExceptionDto.getTcsPersonId());
  }

  @Test
  void shouldNotAddExceptionIfProgrammeMembershipExpiredButDisconnected() {
    ConnectionInfoDto visitorPmExpiredExceptionDto = noExceptionDto;
    visitorPmExpiredExceptionDto.setProgrammeMembershipStartDate(LocalDate.now().minusDays(200));
    visitorPmExpiredExceptionDto.setProgrammeMembershipEndDate(LocalDate.now().minusDays(100));
    visitorPmExpiredExceptionDto.setDesignatedBody(null);
    visitorPmExpiredExceptionDto.setConnectionStatus("No");
    indexUpdateHelper.updateElasticSearchIndex(visitorPmExpiredExceptionDto);
    verify(exceptionElasticSearchService, never()).saveView(
        indexUpdateHelper.getExceptionViews(visitorPmExpiredExceptionDto));
    verify(connectedElasticSearchService, never()).saveView(
        indexUpdateHelper.getConnectedViews(visitorPmExpiredExceptionDto));
    verify(exceptionElasticSearchService).removeViewByGmcReferenceNumber(
        visitorPmExpiredExceptionDto.getGmcReferenceNumber());
    verify(exceptionElasticSearchService).removeViewByTcsPersonId(
        visitorPmExpiredExceptionDto.getTcsPersonId());
    verify(disconnectedElasticSearchService, never()).removeViewByTcsPersonId(
        visitorPmExpiredExceptionDto.getTcsPersonId());
  }

  @Test
  void shouldNotThrowErrorIfProgrammeMembershipTypeNull() {
    ConnectionInfoDto visitorPmExpiredExceptionDto = noExceptionDto;
    visitorPmExpiredExceptionDto.setProgrammeMembershipType(null);
    indexUpdateHelper.updateElasticSearchIndex(visitorPmExpiredExceptionDto);
    verify(exceptionElasticSearchService, never()).saveView(
        indexUpdateHelper.getExceptionViews(visitorPmExpiredExceptionDto));
  }

  @Test
  void shouldNotThrowErrorIfProgrammeMembershipEndDateNull() {
    ConnectionInfoDto visitorPmExpiredExceptionDto = noExceptionDto;
    visitorPmExpiredExceptionDto.setProgrammeMembershipEndDate(null);
    indexUpdateHelper.updateElasticSearchIndex(visitorPmExpiredExceptionDto);
    verify(exceptionElasticSearchService, never()).saveView(
        indexUpdateHelper.getExceptionViews(visitorPmExpiredExceptionDto));
  }

  @Test
  void shouldRemoveExceptionIfNotVisitor() {
    indexUpdateHelper.updateElasticSearchIndex(noExceptionDto);
    verify(exceptionElasticSearchService).removeViewByGmcReferenceNumber(
        noExceptionDto.getGmcReferenceNumber()
    );
    verify(exceptionElasticSearchService).removeViewByTcsPersonId(
        noExceptionDto.getTcsPersonId()
    );
  }

  @Test
  void shouldGetExceptionViews() {
    final ExceptionView returnedView = indexUpdateHelper
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
    final ConnectedView returnedView = indexUpdateHelper
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
    final DisconnectedView returnedView = indexUpdateHelper
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

  @Test
  void shouldClearConnectionIndex() {
    indexUpdateHelper.clearConnectionIndexes(ES_INDICES);
    verify(elasticSearchOperations).deleteIndex("connectedindex");
    verify(elasticSearchOperations).deleteIndex("disconnectedindex");
    verify(elasticSearchOperations).deleteIndex("exceptionindex");
    verify(elasticSearchOperations).putMapping(ConnectedView.class);
    verify(elasticSearchOperations).putMapping(DisconnectedView.class);
    verify(elasticSearchOperations).putMapping(ExceptionView.class);
  }

  @Test
  void shouldNotClearConnectionIndexIfNotMatch() {
    indexUpdateHelper.clearConnectionIndexes(List.of("randomText"));
    verify(elasticSearchOperations, never()).putMapping(ConnectedView.class);
    verify(elasticSearchOperations, never()).putMapping(DisconnectedView.class);
    verify(elasticSearchOperations, never()).putMapping(ExceptionView.class);
  }
}
