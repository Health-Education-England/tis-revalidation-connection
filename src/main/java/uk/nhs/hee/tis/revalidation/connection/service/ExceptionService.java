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

import static java.time.LocalDateTime.now;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.nhs.hee.tis.revalidation.connection.dto.ExceptionLogDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionLog;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.ExceptionRepository;

@Slf4j
@Transactional
@Service
public class ExceptionService {

  @Autowired
  private ExceptionRepository repository;

  private final ConnectionInfoMapper exceptionLogMapper = Mappers
      .getMapper(ConnectionInfoMapper.class);

  /**
   * Create exception log.
   *
   * @param gmcId        gmcId of trainees where there are some issue when updating connection
   * @param errorMessage response code that response from gmc
   */
  public void createExceptionLog(final String gmcId, final String errorMessage, String admin) {

    final var exceptionLog = ExceptionLog.builder().gmcId(gmcId).errorMessage(errorMessage)
        .timestamp(now()).admin(admin).build();

    repository.save(exceptionLog);
  }

  /**
   * Remove exception log.
   *
   * @param gmcId gmcId of trainees that we want to remove from exceptional log
   */
  public void removeExceptionLog(final String gmcId) {
    repository.deleteById(gmcId);
  }

  /**
   * Get today's exception logs for a specific admin.
   *
   * @param admin request by date today
   */
  public List<ExceptionLogDto> getExceptionLogs(String admin) {
    LocalDateTime today = LocalDate.now().atStartOfDay();
    LocalDateTime tomorrow = today.plusDays(1);

    final var todaysExceptions = repository.findByAdminAndTimestampBetween(admin, today, tomorrow);
    return exceptionLogMapper.exceptionLogsToExceptionLogDtos(todaysExceptions);
  }

}
