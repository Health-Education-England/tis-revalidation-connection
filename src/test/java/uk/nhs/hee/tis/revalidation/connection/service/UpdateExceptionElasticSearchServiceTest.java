package uk.nhs.hee.tis.revalidation.connection.service;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.ArrayList;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionView;
import uk.nhs.hee.tis.revalidation.connection.repository.ExceptionElasticSearchRepository;

@ExtendWith(MockitoExtension.class)
class UpdateExceptionElasticSearchServiceTest {

  private static final Long PERSONID = (long) 111;
  private static final String GMCID = "123";
  private static final String SUBSTANTIVE = "Substantive";

  @Mock
  ExceptionElasticSearchRepository repository;

  @InjectMocks
  UpdateExceptionElasticSearchService updateExceptionElasticSearchService;

  private ExceptionView exceptionView = new ExceptionView();
  private ArrayList<ExceptionView> existingRecords = new ArrayList<>();

  /**
   * setup data for testing.
   */
  @BeforeEach
  public void setup() {
    exceptionView = ExceptionView.builder()
        .tcsPersonId(PERSONID)
        .gmcReferenceNumber(GMCID)
        .doctorFirstName("first")
        .doctorLastName("last")
        .submissionDate(LocalDate.now())
        .programmeName("programme")
        .membershipType(SUBSTANTIVE)
        .designatedBody("body")
        .tcsDesignatedBody("tcsbody")
        .programmeOwner("owner")
        .connectionStatus("Yes")
        .build();
  }

  @Test
  void shouldSaveNewExceptionView() {
    BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();
    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();
    shouldQuery
        .should(new MatchQueryBuilder("gmcReferenceNumber", exceptionView.getGmcReferenceNumber()));
    shouldQuery
        .should(new MatchQueryBuilder("tcsPersonId", exceptionView.getTcsPersonId()));
    BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);

    doReturn(existingRecords).when(repository).search(fullQuery);

    updateExceptionElasticSearchService.saveExceptionViews(exceptionView);
    verify(repository).save(exceptionView);
  }

  @Test
  void shouldSaveExceptionViewGmcNumberNull() {
    ExceptionView gmcNumberNullexceptionView = exceptionView;
    gmcNumberNullexceptionView.setGmcReferenceNumber(null);

    BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();
    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();
    shouldQuery
        .should(new MatchQueryBuilder("tcsPersonId", gmcNumberNullexceptionView.getTcsPersonId()));
    BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);

    doReturn(existingRecords).when(repository).search(fullQuery);

    updateExceptionElasticSearchService.saveExceptionViews(gmcNumberNullexceptionView);
    verify(repository).save(gmcNumberNullexceptionView);
  }

  @Test
  void shouldSaveExceptionViewPersonIdNull() {
    ExceptionView gmcNumberNullexceptionView = exceptionView;
    gmcNumberNullexceptionView.setTcsPersonId(null);

    BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();
    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();
    shouldQuery
        .should(new MatchQueryBuilder("-b gmcReferenceNumber",
            gmcNumberNullexceptionView.getGmcReferenceNumber()));
    BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);

    updateExceptionElasticSearchService.saveExceptionViews(gmcNumberNullexceptionView);
    verify(repository).save(gmcNumberNullexceptionView);
  }

  @Test
  void shouldUpdateExistingExceptionView() {
    BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();
    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();
    shouldQuery
        .should(new MatchQueryBuilder("gmcReferenceNumber", exceptionView.getGmcReferenceNumber()));
    shouldQuery
        .should(new MatchQueryBuilder("tcsPersonId", exceptionView.getTcsPersonId()));
    BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);

    existingRecords.add(exceptionView);
    doReturn(existingRecords).when(repository).search(fullQuery);

    exceptionView.setId(existingRecords.get(0).getId());
    updateExceptionElasticSearchService.saveExceptionViews(exceptionView);
    verify(repository).save(exceptionView);
  }

  @Test
  void shouldRemoveExceptionViewByGmcNumber() {
    updateExceptionElasticSearchService.removeExceptionViewByGmcNumber(GMCID);
    verify(repository).deleteByGmcReferenceNumber(GMCID);
  }

  @Test
  void shouldNotRemoveExceptionViewByGmcNumberIfNull() {
    updateExceptionElasticSearchService.removeExceptionViewByGmcNumber(null);
    verify(repository, never()).deleteByGmcReferenceNumber(GMCID);
  }

  @Test
  void shouldRemoveExceptionViewByTcsPersonId() {
    updateExceptionElasticSearchService.removeExceptionViewByTcsPersonId(PERSONID);
    verify(repository).deleteByTcsPersonId(PERSONID);
  }

  @Test
  void shouldNotRemoveExceptionViewByTcsPersonIdIfNull() {
    updateExceptionElasticSearchService.removeExceptionViewByTcsPersonId(null);
    verify(repository, never()).deleteByTcsPersonId(PERSONID);
  }
}
