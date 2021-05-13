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
import static java.util.stream.Collectors.toList;
import static org.springframework.data.domain.PageRequest.of;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.data.domain.Sort.by;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.nhs.hee.tis.revalidation.connection.dto.ExceptionRecordDto;
import uk.nhs.hee.tis.revalidation.connection.dto.ExceptionRequestDto;
import uk.nhs.hee.tis.revalidation.connection.dto.ExceptionResponseDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionLog;
import uk.nhs.hee.tis.revalidation.connection.entity.GmcResponseCode;
import uk.nhs.hee.tis.revalidation.connection.repository.ExceptionRepository;

@Slf4j
@Transactional
@Service
public class ExceptionService {

  @Autowired
  private ExceptionRepository repository;

  /**
   * Create exception log.
   *
   * @param gmcId        gmcId of trainees where there are some issue when updating connection
   * @param responseCode response code that response from gmc
   */
  public void createExceptionLog(final String gmcId, final String responseCode) {
    final String exceptionMessage = GmcResponseCode.fromCode(responseCode).getMessage();
    final var exceptionLog = ExceptionLog.builder().gmcId(gmcId).errorMessage(exceptionMessage)
        .timestamp(now()).build();

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
   * Get exception log.
   *
   * @param requestDto request for getting exception log
   */
  public ExceptionResponseDto getExceptionLog(final ExceptionRequestDto requestDto) {
    final var direction = "asc".equalsIgnoreCase(requestDto.getSortOrder()) ? ASC : DESC;
    final var pageableAndSortable = of(requestDto.getPageNumber(), 20,
        by(direction, requestDto.getSortColumn()));

    final var exceptionLogPage = repository.findAll(pageableAndSortable);
    final var exceptionLogs = exceptionLogPage.get().collect(toList());
    return ExceptionResponseDto.builder()
        .totalPages(exceptionLogPage.getTotalPages())
        .totalResults(exceptionLogPage.getTotalElements())
        .exceptionRecord(buildExceptionRecords(exceptionLogs))
        .build();
  }

  private List<ExceptionRecordDto> buildExceptionRecords(final List<ExceptionLog> exceptionLogs) {
    return exceptionLogs.stream().map(exception -> {
      return ExceptionRecordDto.builder()
          .gmcId(exception.getGmcId())
          .exceptionMessage(exception.getErrorMessage())
          .build();
    }).collect(toList());
  }

  /**
   * Send to exception queue.
   *
   * @param gmcId        gmcId of trainees that we want to send to exception queue
   * @param errorMessage the message that will be saved into repository
   */
  public void sendToExceptionQueue(final String gmcId, final String errorMessage) {
    final var exceptionLog = ExceptionLog.builder().gmcId(gmcId)
        .errorMessage(errorMessage).timestamp(now()).build();
    repository.save(exceptionLog);
  }
}
