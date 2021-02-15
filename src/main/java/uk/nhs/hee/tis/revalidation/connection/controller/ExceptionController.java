package uk.nhs.hee.tis.revalidation.connection.controller;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.tis.revalidation.connection.dto.ExceptionResponseDto;
import uk.nhs.hee.tis.revalidation.connection.service.ExceptionService;
import uk.nhs.hee.tis.revalidation.connection.service.PersonElasticSearchService;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import uk.nhs.hee.tis.revalidation.connection.service.StringConverter;

import static org.springframework.data.domain.PageRequest.of;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.data.domain.Sort.by;
import static uk.nhs.hee.tis.revalidation.connection.service.StringConverter.getConverter;

@RestController
@RequestMapping("/api/exception")
public class ExceptionController {

  private static final String SORT_COLUMN = "sortColumn";
  private static final String SORT_ORDER = "sortOrder";
  private static final String SUBMISSION_DATE = "submissionDate";
//  private static final String DESC = "desc";
  private static final String PAGE_NUMBER = "pageNumber";
  private static final String PAGE_NUMBER_VALUE = "0";
  private static final String DESIGNATED_BODY_CODES = "dbcs";
  private static final String SEARCH_QUERY = "searchQuery";
  private static final String EMPTY_STRING = "";

  @Autowired
  private ExceptionService exceptionService;

  @Autowired
  private PersonElasticSearchService personElasticSearchService;

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
      @RequestParam(name = SORT_COLUMN, defaultValue = SUBMISSION_DATE, required = false) final String sortColumn,
      @RequestParam(name = SORT_ORDER, defaultValue = "desc", required = false) final String sortOrder,
      @RequestParam(name = PAGE_NUMBER, defaultValue = PAGE_NUMBER_VALUE, required = false) final int pageNumber,
      @RequestParam(name = DESIGNATED_BODY_CODES, required = false) final List<String> dbcs,
      @RequestParam(name = SEARCH_QUERY, defaultValue = EMPTY_STRING, required = false) String searchQuery
  ) throws IOException {
      final var direction = "asc".equalsIgnoreCase(sortOrder) ? ASC : DESC;
      final var pageableAndSortable = of(pageNumber, 20,
          by(direction, sortColumn));

      searchQuery = getConverter(searchQuery).fromJson().decodeUrl().escapeForSql().toString();
      String searchQueryES = getConverter(searchQuery).fromJson().decodeUrl().escapeForElasticSearch()
          .toString();
      final ExceptionResponseDto exceptionResponseDto;

    exceptionResponseDto = personElasticSearchService.searchForPage(searchQueryES, pageableAndSortable);

    return ResponseEntity.ok(exceptionResponseDto);

  }
}
