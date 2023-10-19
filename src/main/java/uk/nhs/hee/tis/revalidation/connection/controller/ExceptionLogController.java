package uk.nhs.hee.tis.revalidation.connection.controller;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.tis.revalidation.connection.dto.ExceptionLogDto;
import uk.nhs.hee.tis.revalidation.connection.service.ExceptionService;

@Slf4j
@RestController
@RequestMapping("/api/exceptionLog")
public class ExceptionLogController {
  private ExceptionService exceptionService;

  public ExceptionLogController(ExceptionService exceptionService) {
    this.exceptionService = exceptionService;
  }

  private static final String ADMIN = "admin";

  /**
   * GET  /exceptions/today : get list of exceptions from today for an admin.
   *
   * @return the list of ExceptionLogDto
   */
  @GetMapping("/today")
  public ResponseEntity<List<ExceptionLogDto>> getListOfConnectionExceptionsFromToday(
      @RequestParam(name = ADMIN) final String admin) {

    log.info("Received request to fetch exceptions for admin: {}", admin);
    final var exceptions = exceptionService.getConnectionExceptionLogsFromToday(admin);
    return ResponseEntity.ok().body(exceptions);
  }
}
