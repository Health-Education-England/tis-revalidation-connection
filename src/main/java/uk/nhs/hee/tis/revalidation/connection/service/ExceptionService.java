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
   * @param gmcId gmcId of trainees where there are some issue when updating connection
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

  public ExceptionResponseDto getExceptionLog(final ExceptionRequestDto requestDto) {
    final var direction = "asc".equalsIgnoreCase(requestDto.getSortOrder()) ? ASC : DESC;
    final var pageableAndSortable = of(requestDto.getPageNumber(), 20,
        by(direction, requestDto.getSortColumn()));

    final var exceptionLogPage = repository.findAll(pageableAndSortable);
    final var exceptionLogs = exceptionLogPage.get().collect(toList());
    final var exceptionResponseDto = ExceptionResponseDto.builder()
        .totalPages(exceptionLogPage.getTotalPages())
        .totalResults(exceptionLogPage.getTotalElements())
        .exceptionRecord(buildExceptionRecords(exceptionLogs))
        .build();
    return exceptionResponseDto;
  }

  private List<ExceptionRecordDto> buildExceptionRecords(final List<ExceptionLog> exceptionLogs) {
    return exceptionLogs.stream().map(exception -> {
      return ExceptionRecordDto.builder()
          .gmcId(exception.getGmcId())
          .exceptionMessage(exception.getErrorMessage())
          .build();
    }).collect(toList());
  }

  public void sendToExceptionQueue(final String gmcId, final String errorMessage) {
    final var exceptionLog = ExceptionLog.builder().gmcId(gmcId)
        .errorMessage(errorMessage).timestamp(now()).build();
    repository.save(exceptionLog);
  }
}
