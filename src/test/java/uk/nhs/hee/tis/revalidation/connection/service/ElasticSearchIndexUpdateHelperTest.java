package uk.nhs.hee.tis.revalidation.connection.service;

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

  private ConnectionInfoDto expiredExceptionDto = ConnectionInfoDto.builder()
      .gmcReferenceNumber("123")
      .doctorFirstName("first")
      .doctorLastName("last")
      .submissionDate(LocalDate.now())
      .programmeName("programme")
      .programmeMembershipType(SUBSTANTIVE)
      .designatedBody("body")
      .tcsDesignatedBody("tcsbody")
      .programmeOwner("owner")
      .programmeMembershipStartDate(LocalDate.now().minusDays(100))
      .programmeMembershipEndDate(LocalDate.now().minusDays(10))
      .dataSource("source")
      .build();

  private ConnectionInfoDto visitorExceptionDto = ConnectionInfoDto.builder()
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
    verify(elasticSearchService).addExceptionViews(
        elasticSearchIndexUpdateHelper.getExceptionViews(visitorExceptionDto)
    );
  }

  @Test
  void shouldAddExceptionIfExpired() {
    elasticSearchIndexUpdateHelper.updateElasticSearchIndex(expiredExceptionDto);
    verify(elasticSearchService).addExceptionViews(
        elasticSearchIndexUpdateHelper.getExceptionViews(expiredExceptionDto)
    );
  }

  @Test
  void shouldRemoveExceptionIfNotVisitorOrExpired() {
    elasticSearchIndexUpdateHelper.updateElasticSearchIndex(noExceptionDto);
    verify(elasticSearchService).removeExceptionView(
        noExceptionDto.getGmcReferenceNumber()
    );
  }

  @Test
  void shouldGetExceptionViews() {
    final ExceptionView returnedView = elasticSearchIndexUpdateHelper
        .getExceptionViews(visitorExceptionDto).get(0);

    assert (returnedView.getGmcReferenceNumber())
        .equals(visitorExceptionDto.getGmcReferenceNumber());
    assert (returnedView.getDoctorFirstName()).equals(visitorExceptionDto.getDoctorFirstName());
    assert (returnedView.getDoctorLastName()).equals(visitorExceptionDto.getDoctorLastName());
    assert (returnedView.getSubmissionDate()).equals(visitorExceptionDto.getSubmissionDate());
    assert (returnedView.getProgrammeName()).equals(visitorExceptionDto.getProgrammeName());
    assert (returnedView.getProgrammeMembershipType())
        .equals(visitorExceptionDto.getProgrammeMembershipType());
    assert (returnedView.getDesignatedBody()).equals(visitorExceptionDto.getDesignatedBody());
    assert (returnedView.getTcsDesignatedBody()).equals(visitorExceptionDto.getTcsDesignatedBody());
    assert (returnedView.getProgrammeOwner()).equals(visitorExceptionDto.getProgrammeOwner());
    assert (returnedView.getConnectionStatus()).equals(CONNECTED);
    assert (returnedView.getProgrammeMembershipStartDate())
        .equals(visitorExceptionDto.getProgrammeMembershipStartDate());
    assert (returnedView.getProgrammeMembershipEndDate())
        .equals(visitorExceptionDto.getProgrammeMembershipEndDate());
  }

  @Test
  void shouldReturnPositiveConnectionStatus() {
    final ExceptionView returnedView = elasticSearchIndexUpdateHelper
        .getExceptionViews(noConnectionExceptionDto).get(0);
    assert (returnedView.getConnectionStatus()).equals(DISCONNECTED);
  }
}
