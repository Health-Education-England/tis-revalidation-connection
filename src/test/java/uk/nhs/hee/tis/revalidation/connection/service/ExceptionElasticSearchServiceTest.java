package uk.nhs.hee.tis.revalidation.connection.service;

import static java.time.LocalDate.now;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.by;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.elasticsearch.index.query.BoolQueryBuilder;
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
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionView;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.ExceptionElasticSearchRepository;

@ExtendWith(MockitoExtension.class)
class ExceptionElasticSearchServiceTest {

  private static final String VISITOR = "Visitor";
  private static final String SEARCH_QUERY = "first";
  private static final String PAGE_NUMBER_VALUE = "0";
  private String responseCode;
  private Long personId1;
  private Long personId2;
  private String gmcRef1;
  private String gmcRef2;
  private String firstName1;
  private String firstName2;
  private String lastName1;
  private String lastName2;
  private LocalDate submissionDate1;
  private LocalDate submissionDate2;
  private String designatedBody1;
  private String designatedBody2;
  private String programmeName1;
  private String programmeName2;
  private String programmeOwner1;
  private String programmeOwner2;
  private Page<ExceptionView> searchResult;
  private List<ExceptionView> exceptionViews = new ArrayList<>();
  private final Faker faker = new Faker();

  @Mock
  ExceptionElasticSearchRepository exceptionElasticSearchRepository;

  @Mock
  ConnectionInfoMapper connectionInfoMapper;

  @InjectMocks
  ExceptionElasticSearchService exceptionElasticSearchService;

  /**
   * Set up data for testing.
   */
  @BeforeEach
  public void setup() {

    personId1 = (long) faker.hashCode();
    personId2 = (long) faker.hashCode();
    gmcRef1 = faker.number().digits(8);
    gmcRef2 = faker.number().digits(8);
    firstName1 = faker.name().firstName();
    firstName2 = faker.name().firstName();
    lastName1 = faker.name().lastName();
    lastName2 = faker.name().lastName();
    submissionDate1 = now();
    submissionDate2 = now();
    designatedBody1 = faker.lorem().characters(8);
    designatedBody2 = faker.lorem().characters(8);
    programmeName1 = faker.lorem().characters(20);
    programmeName2 = faker.lorem().characters(20);
    programmeOwner1 = faker.lorem().characters(20);
    programmeOwner2 = faker.lorem().characters(20);


    ExceptionView exceptionView = ExceptionView.builder()
        .tcsPersonId((long) 111)
        .gmcReferenceNumber(gmcRef1)
        .doctorFirstName(firstName1)
        .doctorLastName(lastName1)
        .submissionDate(submissionDate1)
        .programmeName(programmeName1)
        .designatedBody(designatedBody1)
        .programmeOwner(programmeOwner1)
        .build();
    exceptionViews.add(exceptionView);
    searchResult = new PageImpl<>(exceptionViews);
  }

  @Test
  void shouldSearchForPage() {
    BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();
    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();
    BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);
    final var pageableAndSortable = PageRequest.of(Integer.parseInt(PAGE_NUMBER_VALUE), 20,
        by(ASC, "gmcReferenceNumber.keyword"));

    when(exceptionElasticSearchRepository.search(fullQuery, pageableAndSortable))
        .thenReturn(searchResult);

    final var records = searchResult.get().collect(toList());
    var connectionSummary = ConnectionSummaryDto.builder()
        .totalPages(searchResult.getTotalPages())
        .totalResults(searchResult.getTotalElements())
        .connections(connectionInfoMapper.exceptionToDtos(records))
        .build();

    ConnectionSummaryDto result = exceptionElasticSearchService
        .searchForPage("", pageableAndSortable);
    assertThat(result, is(connectionSummary));
  }
}
