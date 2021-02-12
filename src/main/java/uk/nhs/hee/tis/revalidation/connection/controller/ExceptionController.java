package uk.nhs.hee.tis.revalidation.connection.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.tis.revalidation.connection.dto.ExceptionRequestDto;
import uk.nhs.hee.tis.revalidation.connection.dto.ExceptionResponseDto;
import uk.nhs.hee.tis.revalidation.connection.dto.PersonViewDTO;
import uk.nhs.hee.tis.revalidation.connection.entity.ColumnFilter;
import uk.nhs.hee.tis.revalidation.connection.entity.PersonView;
import uk.nhs.hee.tis.revalidation.connection.entity.Status;
import uk.nhs.hee.tis.revalidation.connection.service.ColumnFilterUtil;
import uk.nhs.hee.tis.revalidation.connection.service.ExceptionService;
import uk.nhs.hee.tis.revalidation.connection.service.PersonElasticSearchService;
import com.google.common.collect.Lists;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.nhs.hee.tis.revalidation.connection.service.StringConverter;

import static uk.nhs.hee.tis.revalidation.connection.service.StringConverter.getConverter;

@RestController
@RequestMapping("/api/exception")
public class ExceptionController {

  private static final String SORT_COLUMN = "sortColumn";
  private static final String SORT_ORDER = "sortOrder";
  private static final String SUBMISSION_DATE = "submissionDate";
  private static final String DESC = "desc";
  private static final String PAGE_NUMBER = "pageNumber";
  private static final String PAGE_NUMBER_VALUE = "0";

  @Autowired
  private ExceptionService exceptionService;

  @Autowired
  private PersonElasticSearchService personElasticSearchService;

  private static final Logger LOGGER = LoggerFactory.getLogger(StringConverter.class.getName());

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
      final Pageable pageable,
      @RequestParam(value = "searchQuery", required = false) String searchQuery,
      @RequestParam(value = "columnFilters", required = false) final String columnFilterJson//,
/*      @RequestParam(name = SORT_COLUMN, defaultValue = SUBMISSION_DATE, required = false)
      final String sortColumn,
      @RequestParam(name = SORT_ORDER, defaultValue = DESC, required = false)
      final String sortOrder,*/
  /*    @RequestParam(name = PAGE_NUMBER, defaultValue = PAGE_NUMBER_VALUE, required = false)
      final int pageNumber*/) throws IOException {


      searchQuery = getConverter(searchQuery).fromJson().decodeUrl().escapeForSql().toString();
      String searchQueryES = getConverter(searchQuery).fromJson().decodeUrl().escapeForElasticSearch()
          .toString();
      final List<Class> filterEnumList = Lists.newArrayList(Status.class);
      final List<ColumnFilter> columnFilters = ColumnFilterUtil
          .getColumnFilters(columnFilterJson, filterEnumList);
      final Page<PersonViewDTO> page;

      //feature flag to enable es, allow the enabling from the FE
     page = personElasticSearchService.searchForPage(searchQueryES, columnFilters, pageable);

    LOGGER.debug(page.toString());


   /* final var exceptionRequestDto = ExceptionRequestDto.builder().sortColumn(sortColumn)
        .sortOrder(sortOrder)
        .pageNumber(pageNumber).build();

    final var exceptionResponseDto = exceptionService.getExceptionLog(exceptionRequestDto);*/
    return ResponseEntity.ok(ExceptionResponseDto.builder().build());

  }
}
