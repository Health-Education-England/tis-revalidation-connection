package uk.nhs.hee.tis.revalidation.connection.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.by;

import com.github.javafaker.Faker;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import uk.nhs.hee.tis.revalidation.connection.dto.ExceptionRequestDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionLog;
import uk.nhs.hee.tis.revalidation.connection.repository.ExceptionRepository;

@ExtendWith(MockitoExtension.class)
class ExceptionServiceTest {

  private final Faker faker = new Faker();

  @InjectMocks
  private ExceptionService exceptionService;

  @Mock
  private ExceptionRepository repository;

  @Mock
  private Page page;

  @Mock
  private ExceptionLog exceptionLog;

  private String gmcId;
  private String responseCode;

  @BeforeEach
  public void setup() {
    gmcId = faker.number().digits(8);
    responseCode = "0";
  }

  @Test
  public void shouldCreateExceptionLog() {
    exceptionService.createExceptionLog(gmcId, responseCode);
    verify(repository).save(any(ExceptionLog.class));
  }

  @Test
  public void shouldGetExceptionLog() {
    final var exceptionRequestDto = ExceptionRequestDto.builder().pageNumber(1)
        .sortOrder("asc").sortColumn("gmcId").build();
    final var pageableAndSortable = PageRequest.of(1, 20, by(ASC, "gmcId"));
    when(repository.findAll(pageableAndSortable)).thenReturn(page);
    when(page.get()).thenReturn(Stream.of(exceptionLog));
    final var exceptionResponseDto = exceptionService.getExceptionLog(exceptionRequestDto);
    assertThat(exceptionResponseDto.getExceptionRecord(), hasSize(1));
  }
}
