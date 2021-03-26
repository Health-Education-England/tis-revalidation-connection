package uk.nhs.hee.tis.revalidation.connection.service;

import static org.mockito.Mockito.doReturn;
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
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectedView;
import uk.nhs.hee.tis.revalidation.connection.repository.ConnectedElasticSearchRepository;

@ExtendWith(MockitoExtension.class)
public class UpdateConnectedElasticSearchServiceTest {

  private static final Long PERSONID = (long) 111;
  private static final String GMCID = "123";
  private static final String SUBSTANTIVE = "Substantive";

  @Mock
  ConnectedElasticSearchRepository repository;

  @InjectMocks
  UpdateConnectedElasticSearchService updateConnectedElasticSearchService;

  private ConnectedView connectedView = new ConnectedView();
  private ArrayList<ConnectedView> existingRecords = new ArrayList<>();

  @BeforeEach
  public void setup() {
    connectedView = ConnectedView.builder()
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
  void shouldSaveNewConnectedView() {
    BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();
    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();
    shouldQuery
        .should(new MatchQueryBuilder("gmcReferenceNumber", connectedView.getGmcReferenceNumber()));
    shouldQuery
        .should(new MatchQueryBuilder("tcsPersonId", connectedView.getTcsPersonId()));
    BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);

    doReturn(existingRecords).when(repository).search(fullQuery);

    updateConnectedElasticSearchService.saveConnectedViews(connectedView);
    verify(repository).save(connectedView);
  }

  @Test
  void shouldSaveConnectedViewGmcNumberNull() {
    ConnectedView gmcNumberNullconnectedView = connectedView;
    gmcNumberNullconnectedView.setGmcReferenceNumber(null);

    BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();
    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();
    shouldQuery
        .should(new MatchQueryBuilder("tcsPersonId", gmcNumberNullconnectedView.getTcsPersonId()));
    BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);

    doReturn(existingRecords).when(repository).search(fullQuery);

    updateConnectedElasticSearchService.saveConnectedViews(gmcNumberNullconnectedView);
    verify(repository).save(gmcNumberNullconnectedView);
  }

  @Test
  void shouldSaveConnectedViewPersonIdNull() {
    ConnectedView gmcNumberNullconnectedView = connectedView;
    gmcNumberNullconnectedView.setTcsPersonId(null);

    BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();
    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();
    shouldQuery
        .should(new MatchQueryBuilder("-b gmcReferenceNumber", gmcNumberNullconnectedView.getGmcReferenceNumber()));
    BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);

    updateConnectedElasticSearchService.saveConnectedViews(gmcNumberNullconnectedView);
    verify(repository).save(gmcNumberNullconnectedView);
  }

  @Test
  void shouldUpdateExistingConnectedView() {
    BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();
    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();
    shouldQuery
        .should(new MatchQueryBuilder("gmcReferenceNumber", connectedView.getGmcReferenceNumber()));
    shouldQuery
        .should(new MatchQueryBuilder("tcsPersonId", connectedView.getTcsPersonId()));
    BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);

    existingRecords.add(connectedView);
    doReturn(existingRecords).when(repository).search(fullQuery);

    connectedView.setId(existingRecords.get(0).getId());
    updateConnectedElasticSearchService.saveConnectedViews(connectedView);
    verify(repository).save(connectedView);
  }

  @Test
  void shouldRemoveConnectedViewByGmcNumber() {
    updateConnectedElasticSearchService.removeConnectedViewByGmcNumber(GMCID);
    verify(repository).deleteByGmcReferenceNumber(GMCID);
  }

  @Test
  void shouldRemoveConnectedViewByTcsPersonId() {
    updateConnectedElasticSearchService.removeConnectedViewByTcsPersonId(PERSONID);
    verify(repository).deleteByTcsPersonId(PERSONID);
  }
}
