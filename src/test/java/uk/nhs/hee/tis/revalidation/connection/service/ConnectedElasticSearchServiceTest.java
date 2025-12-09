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
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.by;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.util.List;
import org.assertj.core.util.Lists;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import uk.nhs.hee.tis.revalidation.connection.repository.CurrentConnectionElasticSearchRepository;


@ExtendWith(MockitoExtension.class)
class ConnectedElasticSearchServiceTest {

  private static final String VISITOR = "Visitor";
  private static final String PAGE_NUMBER_VALUE = "0";
  private final Faker faker = new Faker();
  @Mock
  CurrentConnectionElasticSearchRepository currentConnectionElasticSearchRepository;
  @Mock
  ConnectionInfoMapper connectionInfoMapper;
  @Mock
  private ElasticsearchOperations elasticsearchOperations;
  @InjectMocks
  ConnectedElasticSearchService connectedElasticSearchService;
  private String gmcRef1;
  private String firstName1;
  private String lastName1;
  private LocalDate submissionDate1;
  private String designatedBody1;
  private String designatedBody2;
  private String programmeName1;
  private String programmeOwner1;
  private String exceptionReason1;
  private Page<CurrentConnectionsView> currentConnectionsSearchResult;

  /**
   * Set up data for testing.
   */
  @BeforeEach
  public void setup() {

    gmcRef1 = faker.number().digits(8);
    firstName1 = faker.name().firstName();
    lastName1 = faker.name().lastName();
    submissionDate1 = now();
    designatedBody1 = "1-1RSSPZ7";
    designatedBody2 = "1-1RSSQ1B";
    programmeName1 = faker.lorem().characters(20);
    programmeOwner1 = faker.lorem().characters(20);
    exceptionReason1 = faker.lorem().characters(20);

    CurrentConnectionsView currentConnectionsView = CurrentConnectionsView.builder()
        .tcsPersonId((long) 111)
        .gmcReferenceNumber(gmcRef1)
        .doctorFirstName(firstName1)
        .doctorLastName(lastName1)
        .submissionDate(submissionDate1)
        .programmeName(programmeName1)
        .designatedBody(designatedBody1)
        .programmeOwner(programmeOwner1)
        .exceptionReason(exceptionReason1)
        .build();
    currentConnectionsSearchResult = new PageImpl<>(Lists.list(currentConnectionsView));
  }

  @Test
  void shouldSearchForPage() throws ConnectionQueryException {
    final var pageableAndSortable = PageRequest.of(Integer.parseInt(PAGE_NUMBER_VALUE), 20,
        by(ASC, "gmcReferenceNumber"));
    final List<String> dbcs = List.of(designatedBody1, designatedBody2);
    final String formattedDbcs = "1rsspz7 1rssq1b";

    when(currentConnectionElasticSearchRepository.findAll("", formattedDbcs,
        "", pageableAndSortable))
        .thenReturn(currentConnectionsSearchResult);

    final var records = currentConnectionsSearchResult.get().collect(toList());
    var connectionSummary = ConnectionSummaryDto.builder()
        .totalPages(currentConnectionsSearchResult.getTotalPages())
        .totalResults(currentConnectionsSearchResult.getTotalElements())
        .connections(connectionInfoMapper.currentConnectionsToConnectionInfoDtos(records))
        .build();

    ConnectionSummaryDto result = connectedElasticSearchService
        .searchForPage("", dbcs, "", pageableAndSortable);
    assertThat(result, is(connectionSummary));
  }

  @Test
  void shouldSearchForPageWithQuery() throws ConnectionQueryException {
    String searchQuery = gmcRef1;
    final var pageableAndSortable = PageRequest.of(Integer.parseInt(PAGE_NUMBER_VALUE), 20,
        by(ASC, "gmcReferenceNumber"));
    final List<String> dbcs = List.of(designatedBody1, designatedBody2);
    final String formattedDbcs = "1rsspz7 1rssq1b";

    when(currentConnectionElasticSearchRepository.findAll(searchQuery, formattedDbcs,
        programmeName1, pageableAndSortable))
        .thenReturn(currentConnectionsSearchResult);

    final var records = currentConnectionsSearchResult.get().collect(toList());
    var connectionSummary = ConnectionSummaryDto.builder()
        .totalPages(currentConnectionsSearchResult.getTotalPages())
        .totalResults(currentConnectionsSearchResult.getTotalElements())
        .connections(connectionInfoMapper.currentConnectionsToConnectionInfoDtos(records))
        .build();

    ConnectionSummaryDto result = connectedElasticSearchService
        .searchForPage(searchQuery, dbcs, programmeName1, pageableAndSortable);
    assertThat(result, is(connectionSummary));
  }

  @Test
  void shouldThrowRuntimeExceptionWhenSearchForPageWithQuery() {
    String searchQuery = gmcRef1;
    final var pageableAndSortable = PageRequest.of(Integer.parseInt(PAGE_NUMBER_VALUE), 20,
        by(ASC, "gmcReferenceNumber"));
    final List<String> dbcs = List.of(designatedBody1, designatedBody2);
    final String formattedDbcs = "1rsspz7 1rssq1b";

    when(currentConnectionElasticSearchRepository.findAll(searchQuery, formattedDbcs,
        programmeName1, pageableAndSortable))
        .thenThrow(RuntimeException.class);

    assertThrows(ConnectionQueryException.class, () -> connectedElasticSearchService
        .searchForPage(searchQuery, dbcs, programmeName1, pageableAndSortable));
  }

  @Test
  void shouldSearchForPageWithMembershipEndDateFromToAndReturnConnectionSummary()
      throws Exception {

    final String searchQuery = "smith";
    final List<String> dbcs = List.of("DB1", "DB2");
    final String programmeName = "Programme1";
    final LocalDate from = LocalDate.of(2024, 1, 1);
    final LocalDate to = LocalDate.of(2024, 12, 31);
    final Pageable pageable = PageRequest.of(0, 20);

    CurrentConnectionsView entity = new CurrentConnectionsView();
    entity.setId("1L");
    entity.setGmcReferenceNumber("1234567");

    @SuppressWarnings("unchecked")
    SearchHit<CurrentConnectionsView> hit =
        (SearchHit<CurrentConnectionsView>) mock(SearchHit.class);
    when(hit.getContent()).thenReturn(entity);

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
            searchQuery, dbcs, programmeName, from, to, pageable);

    assertThat(result, notNullValue());
    assertThat(result.getTotalResults(), is(1L));
    assertThat(result.getTotalPages(), is(1L));
    assertThat(result.getConnections(), hasSize(1));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<NativeSearchQuery> queryCaptor =
        ArgumentCaptor.forClass(NativeSearchQuery.class);

    verify(elasticsearchOperations)
        .search(queryCaptor.capture(), eq(CurrentConnectionsView.class));

    QueryBuilder qb = queryCaptor.getValue().getQuery();
    String queryString = qb.toString();

    assertThat(queryString, containsString("membershipEndDate"));
    assertThat(queryString, containsString(from.toString()));
    assertThat(queryString, containsString(to.toString()));
  }
}
