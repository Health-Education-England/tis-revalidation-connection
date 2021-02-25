package uk.nhs.hee.tis.revalidation.connection.controller;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.tis.revalidation.connection.dto.ExceptionRequestDto;
import uk.nhs.hee.tis.revalidation.connection.dto.ExceptionResponseDto;
import uk.nhs.hee.tis.revalidation.connection.dto.ExceptionSummaryDto;
import uk.nhs.hee.tis.revalidation.connection.service.ExceptionService;

@RestController
@RequestMapping("/api/exception")
public class ExceptionController {

  private static final String SORT_COLUMN = "sortColumn";
  private static final String SORT_ORDER = "sortOrder";
  private static final String SUBMISSION_DATE = "submissionDate";
  private static final String DESC = "desc";
  private static final String PAGE_NUMBER = "pageNumber";
  private static final String PAGE_NUMBER_VALUE = "0";
  private static final String DESIGNATED_BODY_CODES = "dbcs";
  private static final String SEARCH_QUERY = "searchQuery";
  private static final String EMPTY_STRING = "";

  @Autowired
  private ExceptionService exceptionService;

  /**
   * GET  /exception : get exceptions.
   *
   * @param sortColumn column to be sorted
   * @param sortOrder sorting order (ASC or DESC)
   * @param pageNumber page number of data to get
   * @return the ResponseEntity with status 200 (OK) and exception response in body
   */
  @GetMapping
  public ResponseEntity<ExceptionResponseDto> getExceptions(
      @RequestParam(name = SORT_COLUMN, defaultValue = SUBMISSION_DATE, required = false)
      final String sortColumn,
      @RequestParam(name = SORT_ORDER, defaultValue = DESC, required = false)
      final String sortOrder,
      @RequestParam(name = PAGE_NUMBER, defaultValue = PAGE_NUMBER_VALUE, required = false)
      final int pageNumber) {

    final var exceptionRequestDto = ExceptionRequestDto.builder().sortColumn(sortColumn)
        .sortOrder(sortOrder)
        .pageNumber(pageNumber).build();

    final var exceptionResponseDto = exceptionService.getExceptionLog(exceptionRequestDto);
    return ResponseEntity.ok(exceptionResponseDto);

  }


}
