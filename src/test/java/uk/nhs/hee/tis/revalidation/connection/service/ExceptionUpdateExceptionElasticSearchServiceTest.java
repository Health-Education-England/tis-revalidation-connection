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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import uk.nhs.hee.tis.revalidation.connection.entity.DiscrepanciesView;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.DiscrepanciesElasticSearchRepository;

@ExtendWith(MockitoExtension.class)
class ExceptionUpdateExceptionElasticSearchServiceTest {

  private static final String GMCID = "123";
  private static final String SUBSTANTIVE = "Substantive";
  private static final String TEST_QUERY =
      "sortColumn=gmcReferenceNumber&sortOrder=asc&pageNumber=0&filter=exceptionsQueue"
          + "&dbcs=1-AIIDWA,1-AIIDVS,1-AIIDWI,1-AIIDR8,1-AIIDMY,1-AIIDSA,1-AIIDWT";

  @Mock
  DiscrepanciesElasticSearchRepository discrepanciesElasticSearchRepository;
  // By creating this mock, any invocations return null.  I think this is unintentional
  @Mock
  ConnectionInfoMapper connectionInfoMapper;
  @Captor
  private ArgumentCaptor<String> queryCaptor;
  @InjectMocks
  ExceptionElasticSearchService exceptionElasticSearchService;
  private Page<DiscrepanciesView> searchResult;

  /**
   * setup data for testing.
   */
  @BeforeEach
  public void setup() {
    DiscrepanciesView exceptionView = DiscrepanciesView.builder()
        .tcsPersonId((long) 111)
        .gmcReferenceNumber(GMCID)
        .doctorFirstName("first")
        .doctorLastName("last")
        .submissionDate(LocalDate.now())
        .programmeName("programme")
        .membershipType(SUBSTANTIVE)
        .designatedBody("body")
        .tcsDesignatedBody("tcsbody")
        .programmeOwner("owner")
        .exceptionReason("exception reason")
        .build();
    searchResult = new PageImpl<>(List.of(exceptionView));

  }

  @Test
  void shouldUseElasticSearchRepository() {
    when(discrepanciesElasticSearchRepository.findAll(any(String.class), any(Pageable.class)))
        .thenReturn(searchResult);
    exceptionElasticSearchService.searchForPage(TEST_QUERY, Pageable.unpaged());
    verify(discrepanciesElasticSearchRepository)
        .findAll(queryCaptor.capture(), any(Pageable.class));
    assertEquals(TEST_QUERY, queryCaptor.getValue());
  }
}
