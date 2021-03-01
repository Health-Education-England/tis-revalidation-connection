package uk.nhs.hee.tis.revalidation.connection.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionView;
import uk.nhs.hee.tis.revalidation.connection.repository.ExceptionElasticSearchRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class ElasticSearchServiceTest {

  private static final String GMCID = "123";
  private static final String SUBSTANTIVE = "Substantive";

  @Mock
  ExceptionElasticSearchRepository repository;

  @InjectMocks
  ElasticSearchService elasticSearchService;

  private final List<ExceptionView> exceptionViews = new ArrayList<>();
  private final List<ExceptionView> emptyList = new ArrayList<>();

  @BeforeEach
  public void setup() {
    ExceptionView exceptionView = ExceptionView.builder()
        .gmcReferenceNumber(GMCID)
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
        .programmeMembershipEndDate(LocalDate.now().minusDays(10))
        .build();
    exceptionViews.add(exceptionView);
  }

  @Test
  void shouldAddExceptionView() {
    elasticSearchService.addExceptionViews(exceptionViews);
    verify(repository).saveAll(any(ArrayList.class));
  }

  @Test
  void shouldRemoveExceptionView() {
        elasticSearchService.removeExceptionView(GMCID);
        verify(repository).deleteById(GMCID);

  }

  @Test
  void shouldCheckForEmptyList() {
    elasticSearchService.addExceptionViews(emptyList);
    verify(repository, never()).saveAll(any(ArrayList.class));
  }
}
