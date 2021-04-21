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
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectedView;
import uk.nhs.hee.tis.revalidation.connection.repository.ConnectedElasticSearchRepository;

@ExtendWith(MockitoExtension.class)
class UpdateConnectedElasticSearchServiceTest {

  private static final Long PERSONID = (long) 111;
  private static final String GMCID = "123";
  private static final String SUBSTANTIVE = "Substantive";

  @Mock
  ConnectedElasticSearchRepository repository;

  @InjectMocks
  ConnectedElasticSearchService connectedElasticSearchService;

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

    doReturn(existingRecords).when(repository).search(fullQuery);

    connectedElasticSearchService.saveConnectedViews(connectedView);
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

    connectedElasticSearchService.saveConnectedViews(gmcNumberNullconnectedView);
    verify(repository).save(gmcNumberNullconnectedView);
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

    connectedElasticSearchService.saveConnectedViews(gmcNumberNullconnectedView);
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
    connectedElasticSearchService.saveConnectedViews(connectedView);
    verify(repository).save(connectedView);
  }

  @Test
  void shouldRemoveConnectedViewByGmcNumber() {
    connectedElasticSearchService.removeConnectedViewByGmcNumber(GMCID);
    verify(repository).deleteByGmcReferenceNumber(GMCID);
  }

  @Test
  void shouldNotRemoveConnectedViewByGmcNumberIfNull() {
    connectedElasticSearchService.removeConnectedViewByGmcNumber(null);
    verify(repository, never()).deleteByGmcReferenceNumber(GMCID);
  }

  @Test
  void shouldRemoveConnectedViewByTcsPersonId() {
    connectedElasticSearchService.removeConnectedViewByTcsPersonId(PERSONID);
    verify(repository).deleteByTcsPersonId(PERSONID);
  }

  @Test
  void shouldNotRemoveConnectedViewByTcsPersonIdIfNull() {
    connectedElasticSearchService.removeConnectedViewByTcsPersonId(null);
    verify(repository, never()).deleteByTcsPersonId(PERSONID);
  }
}
