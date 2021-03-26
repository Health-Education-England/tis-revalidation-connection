package uk.nhs.hee.tis.revalidation.connection.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionView;

@ExtendWith(MockitoExtension.class)
public class ElasticSearchIndexUpdateHelperTest {

  private static final String VISITOR = "Visitor";
  private static final String SUBSTANTIVE = "Substantive";
  private static final String CONNECTED = "Yes";
  private static final String DISCONNECTED = "No";



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
      .connectionStatus("status")
      .programmeMembershipStartDate(LocalDate.now().minusDays(100))
      .programmeMembershipEndDate(LocalDate.now().plusDays(100))
      .dataSource("source")
      .build();

  @Mock
  ElasticSearchService elasticSearchService;

  @InjectMocks
  ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper;

  @Test
  void shouldAddExceptionIfVisitor() {
    elasticSearchIndexUpdateHelper.updateElasticSearchIndex(visitorExceptionDto);
    verify(elasticSearchService).saveExceptionViews(
        elasticSearchIndexUpdateHelper.getExceptionViews(visitorExceptionDto)
    );
  }

  @Test
  void shouldAddExceptionIfProgrammeMembershipExpired() {
    ConnectionInfoDto pmExpiredExceptionDto = noExceptionDto;
    pmExpiredExceptionDto.setProgrammeMembershipStartDate(LocalDate.now().minusDays(200));
    pmExpiredExceptionDto.setProgrammeMembershipEndDate(LocalDate.now().minusDays(100));
    elasticSearchIndexUpdateHelper.updateElasticSearchIndex(pmExpiredExceptionDto);
    verify(elasticSearchService).saveExceptionViews(
        elasticSearchIndexUpdateHelper.getExceptionViews(pmExpiredExceptionDto)
    );
  }

  @Test
  void shouldAddExceptionIfVisitorAndProgrammeMembershipExpired() {
    ConnectionInfoDto visitorPmExpiredExceptionDto = noExceptionDto;
    visitorPmExpiredExceptionDto.setProgrammeMembershipType(VISITOR);
    visitorPmExpiredExceptionDto.setProgrammeMembershipStartDate(LocalDate.now().minusDays(200));
    visitorPmExpiredExceptionDto.setProgrammeMembershipEndDate(LocalDate.now().minusDays(100));
    elasticSearchIndexUpdateHelper.updateElasticSearchIndex(visitorPmExpiredExceptionDto);
    verify(elasticSearchService).saveExceptionViews(
        elasticSearchIndexUpdateHelper.getExceptionViews(visitorPmExpiredExceptionDto)
    );
  }

  @Test
  void shouldNotRemoveExceptionIfGmcReferenceNumberNull() {
    ConnectionInfoDto noGmcExceptionDto = noExceptionDto;
    noGmcExceptionDto.setGmcReferenceNumber(null);
    elasticSearchIndexUpdateHelper.updateElasticSearchIndex(noGmcExceptionDto);
    verify(elasticSearchService, never()).removeExceptionViewByGmcNumber(
        noGmcExceptionDto.getGmcReferenceNumber()
    );
    verify(elasticSearchService).removeExceptionViewByTcsPersonId(
        noGmcExceptionDto.getTcsPersonId()
    );
  }

  @Test
  void shouldNotRemoveExceptionIfTcsPersonIdNull() {
    ConnectionInfoDto noPersonIdExceptionDto = noExceptionDto;
    noPersonIdExceptionDto.setTcsPersonId(null);
    elasticSearchIndexUpdateHelper.updateElasticSearchIndex(noPersonIdExceptionDto);
    verify(elasticSearchService, never()).removeExceptionViewByTcsPersonId(
        noPersonIdExceptionDto.getTcsPersonId()
    );
    verify(elasticSearchService).removeExceptionViewByGmcNumber(
        noPersonIdExceptionDto.getGmcReferenceNumber()
    );
  }

  @Test
  void shouldRemoveExceptionIfNotVisitor() {
    elasticSearchIndexUpdateHelper.updateElasticSearchIndex(noExceptionDto);
    verify(elasticSearchService).removeExceptionViewByGmcNumber(
        noExceptionDto.getGmcReferenceNumber()
    );
    verify(elasticSearchService).removeExceptionViewByTcsPersonId(
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
    assert (returnedView.getConnectionStatus()).equals(CONNECTED);
  }

  @Test
  void shouldReturnPositiveConnectionStatus() {
    final ExceptionView returnedView = elasticSearchIndexUpdateHelper
        .getExceptionViews(noConnectionExceptionDto);
    assert (returnedView.getConnectionStatus()).equals(DISCONNECTED);
  }
}
