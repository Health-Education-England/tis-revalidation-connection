package uk.nhs.hee.tis.revalidation.connection.controller;

import static java.util.List.of;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javafaker.Faker;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.nhs.hee.tis.revalidation.connection.dto.ExceptionLogDto;
import uk.nhs.hee.tis.revalidation.connection.service.ExceptionService;

@WebMvcTest(ExceptionLogController.class)
class ExceptionLogControllerTest {

  private final Faker faker = new Faker();
  private String admin;
  private LocalDateTime today;
  private String gmcId1;
  private String gmcId2;
  private String exceptionReason1;
  private String exceptionReason2;
  @Autowired
  private MockMvc mockMvc;
  @MockBean
  private ExceptionService exceptionService;
  @InjectMocks
  private ExceptionLogController exceptionLogController;

  @BeforeEach
  public void setup() {

    admin = faker.internet().emailAddress();
    today = LocalDateTime.now();
    gmcId1 = faker.number().digits(8);
    gmcId2 = faker.number().digits(8);
    exceptionReason1 = faker.lorem().characters(20);
    exceptionReason2 = faker.lorem().characters(20);
  }

  @Test
  void shouldReturnAllExceptionsFromTodayForAnAdmin() throws Exception {
    final var exceptionRecordDtoList = buildExceptionRecordDtoList();

    when(exceptionService.getConnectionExceptionLogsFromToday(admin)).thenReturn(
        exceptionRecordDtoList);

    mockMvc.perform(get("/api/exceptionLog/today")
            .param("admin", admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.[*].gmcId").value(hasItem(gmcId1)))
        .andExpect(jsonPath("$.[*].errorMessage").value(hasItem(exceptionReason1)))
        .andExpect(jsonPath("$.[*].admin").value(hasItem(admin)))
        .andExpect(jsonPath("$.[*].gmcId").value(hasItem(gmcId2)))
        .andExpect(jsonPath("$.[*].errorMessage").value(hasItem(exceptionReason2)))
        .andExpect(jsonPath("$.[*].admin").value(hasItem(admin)));

  }

  private List<ExceptionLogDto> buildExceptionRecordDtoList() {
    final var record1 = ExceptionLogDto.builder()
        .gmcId(gmcId1).errorMessage(exceptionReason1)
        .timestamp(today).admin(admin)
        .build();
    final var record2 = ExceptionLogDto.builder()
        .gmcId(gmcId2).errorMessage(exceptionReason2)
        .timestamp(today).admin(admin)
        .build();
    return of(record1, record2);
  }
}
