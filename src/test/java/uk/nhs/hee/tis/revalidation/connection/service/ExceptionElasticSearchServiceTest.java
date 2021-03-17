package uk.nhs.hee.tis.revalidation.connection.service;

import org.elasticsearch.index.query.QueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import uk.nhs.hee.tis.revalidation.connection.dto.ExceptionSummaryDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionView;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.ExceptionElasticSearchRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExceptionElasticSearchServiceTest {

  @Mock
  ExceptionElasticSearchRepository exceptionElasticSearchRepository;

  @Mock
  ConnectionInfoMapper connectionInfoMapper;

  @InjectMocks
  ExceptionElasticSearchService exceptionElasticSearchService;

  private Page<ExceptionView> searchResult;
  private List<ExceptionView> exceptionViews = new ArrayList<>();

  private static final String GMCID = "123";
  private static final String SUBSTANTIVE = "Substantive";
  private static final String TEST_QUERY =
      "sortColumn=gmcReferenceNumber&sortOrder=asc&pageNumber=0&filter=exceptionsQueue&dbcs=1-AIIDWA,1-AIIDVS,1-AIIDWI,1-AIIDR8,1-AIIDMY,1-AIIDSA,1-AIIDWT";

  @BeforeEach
  public void setup() {
    ExceptionView exceptionView = ExceptionView.builder()
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
    exceptionViews.add(exceptionView);
    searchResult = new PageImpl<>(exceptionViews);

  }

  @Test
  void shouldUseElasticSearchRepository() {
    doReturn(searchResult).when(exceptionElasticSearchRepository).search(any(QueryBuilder.class), any(Pageable.class));
    exceptionElasticSearchService.searchForPage(TEST_QUERY, Pageable.unpaged());
    verify(exceptionElasticSearchRepository).search(any(QueryBuilder.class), any(Pageable.class));
  }
}
