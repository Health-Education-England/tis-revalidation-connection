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

import static java.time.LocalDate.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.util.List;
import org.elasticsearch.index.query.QueryBuilder;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionSummaryDto;
import uk.nhs.hee.tis.revalidation.connection.entity.CurrentConnectionsView;
import uk.nhs.hee.tis.revalidation.connection.exception.ConnectionQueryException;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;

@ExtendWith(MockitoExtension.class)
class ConnectedElasticSearchServiceTest {

  private final Faker faker = new Faker();
  @Mock
  ConnectionInfoMapper connectionInfoMapper;
  @Mock
  private ElasticsearchOperations elasticsearchOperations;
  @InjectMocks
  ConnectedElasticSearchService connectedElasticSearchService;
  private String gmcRef;
  private String firstName;
  private String lastName;
  private LocalDate submissionDate;
  private String designatedBody1;
  private String designatedBody2;
  private String programmeName;
  private String programmeOwner;
  private String exceptionReason;
  private CurrentConnectionsView currentConnectionsView;
  private String searchQuery;
  private List<String> dbcs;
  private LocalDate membershipFrom;
  private LocalDate membershipTo;
  private LocalDate submissionFrom;
  private LocalDate submissionTo;
  private LocalDate lastConnectionFrom;
  private LocalDate lastConnectionTo;
  private Pageable pageable;

  /**
   * Set up data for testing.
   */
  @BeforeEach
  public void setup() {

    gmcRef = faker.number().digits(8);
    firstName = faker.name().firstName();
    lastName = faker.name().lastName();
    submissionDate = now();
    designatedBody1 = "1-1RSSPZ7";
    designatedBody2 = "1-1RSSQ1B";
    programmeName = faker.lorem().characters(20);
    programmeOwner = faker.lorem().characters(20);
    exceptionReason = faker.lorem().characters(20);
    searchQuery = "smith";
    dbcs = List.of(designatedBody1, designatedBody2);
    membershipFrom = LocalDate.of(2024, 1, 1);
    membershipTo = LocalDate.of(2024, 12, 31);
    submissionFrom = LocalDate.of(2024, 1, 2);
    submissionTo = LocalDate.of(2024, 12, 30);
    lastConnectionFrom = LocalDate.of(2024, 1, 3);
    lastConnectionTo = LocalDate.of(2024, 12, 29);
    pageable = PageRequest.of(0, 20);

    currentConnectionsView = CurrentConnectionsView.builder()
        .tcsPersonId((long) 111)
        .gmcReferenceNumber(gmcRef)
        .doctorFirstName(firstName)
        .doctorLastName(lastName)
        .submissionDate(submissionDate)
        .programmeName(programmeName)
        .designatedBody(designatedBody1)
        .programmeOwner(programmeOwner)
        .exceptionReason(exceptionReason)
        .build();
  }

  @Test
  void shouldSearchForPageWithMembershipEndDateFromToAndReturnConnectionSummary()
      throws Exception {

    @SuppressWarnings("unchecked")
    SearchHit<CurrentConnectionsView> hit =
        (SearchHit<CurrentConnectionsView>) mock(SearchHit.class);
    when(hit.getContent()).thenReturn(currentConnectionsView);

    @SuppressWarnings("unchecked")
    SearchHits<CurrentConnectionsView> hits =
        (SearchHits<CurrentConnectionsView>) mock(SearchHits.class);
    when(hits.getSearchHits()).thenReturn(List.of(hit));
    when(hits.getTotalHits()).thenReturn(1L);

    when(elasticsearchOperations.search((Query) any(), eq(CurrentConnectionsView.class)))
        .thenReturn(hits);

    List<ConnectionInfoDto> mappedDtos = List.of(new ConnectionInfoDto());
    when(connectionInfoMapper.currentConnectionsToConnectionInfoDtos(anyList()))
        .thenReturn(mappedDtos);

    ConnectionSummaryDto result = connectedElasticSearchService
        .searchForConnectionPageWithFilters(
            searchQuery, dbcs, programmeName, membershipFrom, membershipTo, submissionFrom,
            submissionTo, lastConnectionFrom, lastConnectionTo, pageable);

    assertThat(result, notNullValue());
    assertThat(result.getTotalResults(), is(1L));
    assertThat(result.getTotalPages(), is(1L));
    assertThat(result.getConnections(), hasSize(1));

    ArgumentCaptor<NativeSearchQuery> queryCaptor =
        ArgumentCaptor.forClass(NativeSearchQuery.class);

    verify(elasticsearchOperations)
        .search(queryCaptor.capture(), eq(CurrentConnectionsView.class));

    QueryBuilder qb = queryCaptor.getValue().getQuery();
    String queryString = qb.toString();

    assertThat(queryString, containsString("membershipEndDate"));
    assertThat(queryString, containsString("submissionDate"));
    assertThat(queryString, containsString("lastConnectionDateTime"));
    assertThat(queryString, containsString(membershipFrom.toString()));
    assertThat(queryString, containsString(membershipTo.toString()));
    assertThat(queryString, containsString(submissionFrom.toString()));
    assertThat(queryString, containsString(submissionTo.toString()));
    assertThat(queryString, containsString(lastConnectionFrom.toString()));
    assertThat(queryString, containsString(lastConnectionTo.toString()));
  }

  @Test
  void shouldSearchForPageWithNoDatesProvidedAndReturnConnectionsSummary()
      throws Exception {

    @SuppressWarnings("unchecked")
    SearchHit<CurrentConnectionsView> hit =
        (SearchHit<CurrentConnectionsView>) mock(SearchHit.class);
    when(hit.getContent()).thenReturn(currentConnectionsView);

    @SuppressWarnings("unchecked")
    SearchHits<CurrentConnectionsView> hits =
        (SearchHits<CurrentConnectionsView>) mock(SearchHits.class);
    when(hits.getSearchHits()).thenReturn(List.of(hit));
    when(hits.getTotalHits()).thenReturn(1L);

    when(elasticsearchOperations.search((Query) any(), eq(CurrentConnectionsView.class)))
        .thenReturn(hits);

    List<ConnectionInfoDto> mappedDtos = List.of(new ConnectionInfoDto());
    when(connectionInfoMapper.currentConnectionsToConnectionInfoDtos(anyList()))
        .thenReturn(mappedDtos);

    ConnectionSummaryDto result = connectedElasticSearchService
        .searchForConnectionPageWithFilters(
            searchQuery, dbcs, programmeName,
            null, null,
            null, null,
            null, null, pageable);

    assertThat(result, notNullValue());
    assertThat(result.getTotalResults(), Matchers.is(1L));
    assertThat(result.getTotalPages(), Matchers.is(1L));
    assertThat(result.getConnections(), hasSize(1));

    ArgumentCaptor<NativeSearchQuery> queryCaptor =
        ArgumentCaptor.forClass(NativeSearchQuery.class);

    verify(elasticsearchOperations)
        .search(queryCaptor.capture(), eq(CurrentConnectionsView.class));

    QueryBuilder qb = queryCaptor.getValue().getQuery();
    String queryString = qb.toString();

    assertThat(queryString, not(containsString("membershipEndDate")));
    assertThat(queryString, not(containsString("submissionDate")));
    assertThat(queryString, not(containsString("lastConnectionDateTime")));
  }

  @Test
  void shouldThrowRuntimeExceptionWhenSearchForConnectionsPage() {

    when(elasticsearchOperations.search((Query) any(), eq(CurrentConnectionsView.class)))
        .thenThrow(new RuntimeException("Elasticsearch failure"));

    assertThrows(ConnectionQueryException.class, () -> connectedElasticSearchService
        .searchForConnectionPageWithFilters(
            searchQuery, dbcs, programmeName, membershipFrom, membershipTo, submissionFrom,
            submissionTo, lastConnectionFrom, lastConnectionTo, pageable));
  }
}
