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
