package uk.nhs.hee.tis.revalidation.connection.service.index;

import static org.mockito.Mockito.*;

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
import org.springframework.data.domain.Pageable;
import uk.nhs.hee.tis.revalidation.connection.entity.BaseConnectionView;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectedView;
import uk.nhs.hee.tis.revalidation.connection.entity.DisconnectedView;
import uk.nhs.hee.tis.revalidation.connection.repository.index.IndexRepositoryImpl;

@ExtendWith(MockitoExtension.class)
public class IndexServiceTest {
  private static final Long PERSONID = (long) 111;
  private static final String GMCID = "123";
  private static final String SUBSTANTIVE = "Substantive";

  @InjectMocks
  IndexServiceImpl<BaseConnectionView> indexService;
  @Mock
  IndexRepositoryImpl<BaseConnectionView> indexRepository;

  private String searchQuery;
  private Pageable pagable;
  private ConnectedView connectedView = new ConnectedView();
  private ArrayList<ConnectedView> existingRecords = new ArrayList<>();

  /**
   * setup data for testing.
   */
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

    doReturn(existingRecords).when(indexRepository).executeQuery(fullQuery);

    indexService.saveView(connectedView);
    verify(indexRepository).save(connectedView);
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

    doReturn(existingRecords).when(indexRepository).executeQuery(fullQuery);

    indexService.saveView(gmcNumberNullconnectedView);
    verify(indexRepository).save(gmcNumberNullconnectedView);
  }

  @Test
  void shouldSaveConnectedViewPersonIdNull() {
    ConnectedView gmcNumberNullconnectedView = connectedView;
    gmcNumberNullconnectedView.setTcsPersonId(null);

    BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();
    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();
    shouldQuery
        .should(new MatchQueryBuilder("-b gmcReferenceNumber",
            gmcNumberNullconnectedView.getGmcReferenceNumber()));
    BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);

    indexService.saveView(gmcNumberNullconnectedView);
    verify(indexRepository).save(gmcNumberNullconnectedView);
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
    doReturn(existingRecords).when(indexRepository).executeQuery(fullQuery);

    connectedView.setId(existingRecords.get(0).getId());
    indexService.saveView(connectedView);
    verify(indexRepository).save(connectedView);
  }

//  @Test
//  void shouldSaveNewDisconnectedView() {
//    BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();
//    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();
//    shouldQuery
//        .should(
//            new MatchQueryBuilder("gmcReferenceNumber", disconnectedView.getGmcReferenceNumber()));
//    shouldQuery
//        .should(new MatchQueryBuilder("tcsPersonId", disconnectedView.getTcsPersonId()));
//    BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);
//
//    doReturn(existingRecords).when(indexRepository).search(fullQuery);
//
//    indexService.saveView(disconnectedView);
//    verify(indexRepository).save(disconnectedView);
//  }
//
//  @Test
//  void shouldSaveDisconnectedViewGmcNumberNull() {
//    DisconnectedView gmcNumberNulldisconnectedView = disconnectedView;
//    gmcNumberNulldisconnectedView.setGmcReferenceNumber(null);
//
//    BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();
//    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();
//    shouldQuery
//        .should(
//            new MatchQueryBuilder("tcsPersonId", gmcNumberNulldisconnectedView.getTcsPersonId()));
//    BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);
//
//    doReturn(existingRecords).when(indexRepository).search(fullQuery);
//
//    indexService.saveView(gmcNumberNulldisconnectedView);
//    verify(indexRepository).save(gmcNumberNulldisconnectedView);
//  }
//
//  @Test
//  void shouldSaveDisconnectedViewPersonIdNull() {
//    DisconnectedView gmcNumberNulldisconnectedView = disconnectedView;
//    gmcNumberNulldisconnectedView.setTcsPersonId(null);
//
//    BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();
//    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();
//    shouldQuery
//        .should(new MatchQueryBuilder("-b gmcReferenceNumber",
//            gmcNumberNulldisconnectedView.getGmcReferenceNumber()));
//    BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);
//
//    disindexService.saveDisconnectedViews(gmcNumberNulldisconnectedView);
//    verify(indexRepository).save(gmcNumberNulldisconnectedView);
//  }
//
//  @Test
//  void shouldUpdateExistingDisconnectedView() {
//    BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();
//    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();
//    shouldQuery
//        .should(
//            new MatchQueryBuilder("gmcReferenceNumber", disconnectedView.getGmcReferenceNumber()));
//    shouldQuery
//        .should(new MatchQueryBuilder("tcsPersonId", disconnectedView.getTcsPersonId()));
//    BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);
//
//    existingRecords.add(disconnectedView);
//    doReturn(existingRecords).when(indexRepository).search(fullQuery);
//
//    disconnectedView.setId(existingRecords.get(0).getId());
//    disindexService.saveDisconnectedViews(disconnectedView);
//    verify(indexRepository).save(disconnectedView);
//  }


  @Test
  void shouldRemoveConnectedViewByGmcNumber() {
    indexService.removeViewByGmcReferenceNumber(GMCID);
    verify(indexRepository).deleteViewByGmcReferenceNumber(GMCID);
  }

  @Test
  void shouldNotRemoveConnectedViewByGmcNumberIfNull() {
    indexService.removeViewByGmcReferenceNumber(null);
    verify(indexRepository, never()).deleteViewByGmcReferenceNumber(GMCID);
  }

  @Test
  void shouldRemoveConnectedViewByTcsPersonId() {
    indexService.removeViewByTcsPersonId(PERSONID);
    verify(indexRepository).deleteViewByTcsPersonId(PERSONID);
  }

  @Test
  void shouldNotRemoveConnectedViewByTcsPersonIdIfNull() {
    indexService.removeViewByTcsPersonId(null);
    verify(indexRepository, never()).deleteViewByTcsPersonId(PERSONID);
  }

}
