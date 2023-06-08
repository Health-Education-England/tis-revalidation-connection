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
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.by;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

    when(currentConnectionElasticSearchRepository.findAll("", formattedDbcs, pageableAndSortable))
        .thenReturn(currentConnectionsSearchResult);

    final var records = currentConnectionsSearchResult.get().collect(toList());
    var connectionSummary = ConnectionSummaryDto.builder()
        .totalPages(currentConnectionsSearchResult.getTotalPages())
        .totalResults(currentConnectionsSearchResult.getTotalElements())
        .connections(connectionInfoMapper.currentConnectionsToConnectionInfoDtos(records))
        .build();

    ConnectionSummaryDto result = connectedElasticSearchService
        .searchForPage("", dbcs, pageableAndSortable);
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
        pageableAndSortable))
        .thenReturn(currentConnectionsSearchResult);

    final var records = currentConnectionsSearchResult.get().collect(toList());
    var connectionSummary = ConnectionSummaryDto.builder()
        .totalPages(currentConnectionsSearchResult.getTotalPages())
        .totalResults(currentConnectionsSearchResult.getTotalElements())
        .connections(connectionInfoMapper.currentConnectionsToConnectionInfoDtos(records))
        .build();

    ConnectionSummaryDto result = connectedElasticSearchService
        .searchForPage(searchQuery, dbcs, pageableAndSortable);
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
        pageableAndSortable))
        .thenThrow(RuntimeException.class);

    assertThrows(ConnectionQueryException.class, () -> connectedElasticSearchService
        .searchForPage(searchQuery, dbcs, pageableAndSortable));
  }
}
