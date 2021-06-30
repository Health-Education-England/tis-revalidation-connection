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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionView;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.ExceptionElasticSearchRepository;

@ExtendWith(MockitoExtension.class)
class ExceptionUpdateExceptionElasticSearchServiceTest {

  private static final String GMCID = "123";
  private static final String SUBSTANTIVE = "Substantive";
  private static final String TEST_QUERY =
      "sortColumn=gmcReferenceNumber&sortOrder=asc&pageNumber=0&filter=exceptionsQueue"
          + "&dbcs=1-AIIDWA,1-AIIDVS,1-AIIDWI,1-AIIDR8,1-AIIDMY,1-AIIDSA,1-AIIDWT";
  @Mock
  ExceptionElasticSearchRepository exceptionElasticSearchRepository;
  @Mock
  ConnectionInfoMapper connectionInfoMapper;
  @InjectMocks
  ExceptionElasticSearchService exceptionElasticSearchService;
  private Page<ExceptionView> searchResult;
  private List<ExceptionView> exceptionViews = new ArrayList<>();

  /**
   * setup data for testing.
   */
  @BeforeEach
  public void setup() {
    ExceptionView exceptionView = ExceptionView.builder()
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
        .connectionStatus("Yes")
        .exceptionReason("exception reason")
        .build();
    exceptionViews.add(exceptionView);
    searchResult = new PageImpl<>(exceptionViews);

  }

  @Test
  void shouldUseElasticSearchRepository() {
    doReturn(searchResult).when(exceptionElasticSearchRepository)
        .search(any(QueryBuilder.class), any(Pageable.class));
    exceptionElasticSearchService.searchForPage(TEST_QUERY, Pageable.unpaged());
    verify(exceptionElasticSearchRepository).search(any(QueryBuilder.class), any(Pageable.class));
  }
}
