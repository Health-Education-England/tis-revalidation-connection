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
import uk.nhs.hee.tis.revalidation.connection.entity.DisconnectedView;
import uk.nhs.hee.tis.revalidation.connection.repository.DisconnectedElasticSearchRepository;

@ExtendWith(MockitoExtension.class)
class UpdateDisconnectedElasticSearchServiceTest {

  private static final Long PERSONID = (long) 111;
  private static final String GMCID = "123";
  private static final String SUBSTANTIVE = "Substantive";

  @Mock
  DisconnectedElasticSearchRepository repository;

  @InjectMocks
  UpdateDisconnectedElasticSearchService updateDisconnectedElasticSearchService;

  private DisconnectedView disconnectedView = new DisconnectedView();
  private ArrayList<DisconnectedView> existingRecords = new ArrayList<>();

  /**
   * setup data for testing.
   */
  @BeforeEach
  public void setup() {
    disconnectedView = DisconnectedView.builder()
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
  void shouldSaveNewDisconnectedView() {
    BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();
    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();
    shouldQuery
        .should(
            new MatchQueryBuilder("gmcReferenceNumber", disconnectedView.getGmcReferenceNumber()));
    shouldQuery
        .should(new MatchQueryBuilder("tcsPersonId", disconnectedView.getTcsPersonId()));
    BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);

    doReturn(existingRecords).when(repository).search(fullQuery);

    updateDisconnectedElasticSearchService.saveDisconnectedViews(disconnectedView);
    verify(repository).save(disconnectedView);
  }

  @Test
  void shouldSaveDisconnectedViewGmcNumberNull() {
    DisconnectedView gmcNumberNulldisconnectedView = disconnectedView;
    gmcNumberNulldisconnectedView.setGmcReferenceNumber(null);

    BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();
    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();
    shouldQuery
        .should(
            new MatchQueryBuilder("tcsPersonId", gmcNumberNulldisconnectedView.getTcsPersonId()));
    BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);

    doReturn(existingRecords).when(repository).search(fullQuery);

    updateDisconnectedElasticSearchService.saveDisconnectedViews(gmcNumberNulldisconnectedView);
    verify(repository).save(gmcNumberNulldisconnectedView);
  }

  @Test
  void shouldSaveDisconnectedViewPersonIdNull() {
    DisconnectedView gmcNumberNulldisconnectedView = disconnectedView;
    gmcNumberNulldisconnectedView.setTcsPersonId(null);

    BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();
    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();
    shouldQuery
        .should(new MatchQueryBuilder("-b gmcReferenceNumber",
            gmcNumberNulldisconnectedView.getGmcReferenceNumber()));
    BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);

    updateDisconnectedElasticSearchService.saveDisconnectedViews(gmcNumberNulldisconnectedView);
    verify(repository).save(gmcNumberNulldisconnectedView);
  }

  @Test
  void shouldUpdateExistingDisconnectedView() {
    BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();
    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();
    shouldQuery
        .should(
            new MatchQueryBuilder("gmcReferenceNumber", disconnectedView.getGmcReferenceNumber()));
    shouldQuery
        .should(new MatchQueryBuilder("tcsPersonId", disconnectedView.getTcsPersonId()));
    BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);

    existingRecords.add(disconnectedView);
    doReturn(existingRecords).when(repository).search(fullQuery);

    disconnectedView.setId(existingRecords.get(0).getId());
    updateDisconnectedElasticSearchService.saveDisconnectedViews(disconnectedView);
    verify(repository).save(disconnectedView);
  }

  @Test
  void shouldRemoveDisconnectedViewByGmcNumber() {
    updateDisconnectedElasticSearchService.removeDisconnectedViewByGmcNumber(GMCID);
    verify(repository).deleteByGmcReferenceNumber(GMCID);
  }

  @Test
  void shouldNotRemoveDisconnectedViewByGmcNumberIfNull() {
    updateDisconnectedElasticSearchService.removeDisconnectedViewByGmcNumber(null);
    verify(repository, never()).deleteByGmcReferenceNumber(GMCID);
  }

  @Test
  void shouldRemoveDisconnectedViewByTcsPersonId() {
    updateDisconnectedElasticSearchService.removeDisconnectedViewByTcsPersonId(PERSONID);
    verify(repository).deleteByTcsPersonId(PERSONID);
  }

  @Test
  void shouldNotRemoveDisconnectedViewByTcsPersonIdIfNull() {
    updateDisconnectedElasticSearchService.removeDisconnectedViewByTcsPersonId(null);
    verify(repository, never()).deleteByTcsPersonId(PERSONID);
  }
}
