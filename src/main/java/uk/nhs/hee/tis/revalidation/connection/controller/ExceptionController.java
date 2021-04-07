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

package uk.nhs.hee.tis.revalidation.connection.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.tis.revalidation.connection.dto.ExceptionRequestDto;
import uk.nhs.hee.tis.revalidation.connection.dto.ExceptionResponseDto;
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
   * @param sortOrder  sorting order (ASC or DESC)
   * @param pageNumber page number of data to get
   * @return the ResponseEntity with status 200 (OK) and exception response in body
   */
  @GetMapping
  public ResponseEntity<ExceptionResponseDto> getExceptions(
      @RequestParam(name = SORT_COLUMN, defaultValue = SUBMISSION_DATE, required = false) final String sortColumn,
      @RequestParam(name = SORT_ORDER, defaultValue = DESC, required = false) final String sortOrder,
      @RequestParam(name = PAGE_NUMBER, defaultValue = PAGE_NUMBER_VALUE, required = false) final int pageNumber) {

    final var exceptionRequestDto = ExceptionRequestDto.builder().sortColumn(sortColumn)
        .sortOrder(sortOrder)
        .pageNumber(pageNumber).build();

    final var exceptionResponseDto = exceptionService.getExceptionLog(exceptionRequestDto);
    return ResponseEntity.ok(exceptionResponseDto);

  }


}
