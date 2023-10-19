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

import static java.util.List.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionLog;
import uk.nhs.hee.tis.revalidation.connection.repository.ExceptionRepository;

@ExtendWith(MockitoExtension.class)
class ExceptionServiceTest {

  private final Faker faker = new Faker();

  @InjectMocks
  private ExceptionService exceptionService;

  @Mock
  private ExceptionRepository repository;

  private String gmcId;
  private String gmcId2;
  private String responseCode;
  private LocalDateTime localDateTime;
  private List<ExceptionLog> exceptionLogList;
  private String admin;
  private String exceptionReason1;
  private String exceptionReason2;
  private LocalDateTime dateTime;

  @BeforeEach
  public void setup() {
    gmcId = faker.number().digits(8);
    gmcId2 = faker.number().digits(8);
    responseCode = "0";
    localDateTime = LocalDateTime.now();
    admin = "admin";
    exceptionReason1 = faker.letterify("aa");
    exceptionReason2 = faker.letterify("bb");
    dateTime = LocalDateTime.now();
    initializeExceptionLogList();
  }

  @Test
  void shouldCreateExceptionLog() {
    exceptionService.createExceptionLog(gmcId, responseCode, admin);
    verify(repository).save(any(ExceptionLog.class));
  }

  @Test
  void shouldReturnExceptionLogs() {
    LocalDateTime todayStart = LocalDate.now().atStartOfDay();
    LocalDateTime tomorrowStart = todayStart.plusDays(1);

    when(repository.findByAdminAndTimestampBetween(admin, todayStart, tomorrowStart))
        .thenReturn(buildExceptionLogList());

    final var result = exceptionService.getConnectionExceptionLogsFromToday(admin);
    assertThat(result.get(0).getGmcId(), is(gmcId));
    assertThat(result.get(0).getAdmin(), is(admin));
    assertThat(result.get(0).getTimestamp(), greaterThan(todayStart));
    assertThat(result.get(0).getTimestamp(), lessThan(tomorrowStart));
  }

  private void initializeExceptionLogList() {
    ExceptionLog exceptionLog = ExceptionLog.builder()
        .gmcId(gmcId)
        .errorMessage(responseCode)
        .timestamp(localDateTime)
        .admin("admin")
        .build();
    exceptionLogList = new ArrayList<>();
    exceptionLogList.add(exceptionLog);
  }

  private List<ExceptionLog> buildExceptionLogList() {
    final var record1 = ExceptionLog.builder()
        .gmcId(gmcId).errorMessage(exceptionReason1)
        .timestamp(dateTime).admin(admin)
        .build();
    final var record2 = ExceptionLog.builder()
        .gmcId(gmcId2).errorMessage(exceptionReason2)
        .timestamp(dateTime).admin(admin)
        .build();
    return of(record1, record2);
  }
}
