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

import static java.util.List.of;
import static org.springframework.data.domain.PageRequest.of;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.data.domain.Sort.by;
import static uk.nhs.hee.tis.revalidation.connection.service.StringConverter.getConverter;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionDto;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionSummaryDto;
import uk.nhs.hee.tis.revalidation.connection.dto.ExceptionLogDto;
import uk.nhs.hee.tis.revalidation.connection.dto.UpdateConnectionDto;
import uk.nhs.hee.tis.revalidation.connection.dto.UpdateConnectionResponseDto;
import uk.nhs.hee.tis.revalidation.connection.exception.ConnectionQueryException;
import uk.nhs.hee.tis.revalidation.connection.service.ConnectedElasticSearchService;
import uk.nhs.hee.tis.revalidation.connection.service.ConnectionService;
import uk.nhs.hee.tis.revalidation.connection.service.DisconnectedElasticSearchService;
import uk.nhs.hee.tis.revalidation.connection.service.DiscrepanciesElasticSearchService;
import uk.nhs.hee.tis.revalidation.connection.service.ElasticsearchQueryHelper;
import uk.nhs.hee.tis.revalidation.connection.service.ExceptionService;

@Slf4j
@RestController
@RequestMapping("/api/connections")
public class ConnectionController {

  private static final String SORT_COLUMN = "sortColumn";
  private static final String SORT_ORDER = "sortOrder";
  private static final String GMC_REFERENCE_NUMBER = "gmcReferenceNumber";
  private static final String PAGE_NUMBER = "pageNumber";
  private static final String PAGE_NUMBER_VALUE = "0";
  private static final String DESIGNATED_BODY_CODES = "dbcs";
  private static final String SEARCH_QUERY = "searchQuery";
  private static final String EMPTY_STRING = "";
  private static final String PROGRAMME_NAME = "programmeName";
  private static final String ADMIN = "admin";

  @Autowired
  private ConnectionService connectionService;

  @Autowired
  private DiscrepanciesElasticSearchService discrepanciesElasticSearchService;

  @Autowired
  private ConnectedElasticSearchService connectedElasticSearchService;

  @Autowired
  private DisconnectedElasticSearchService disconnectedElasticSearchService;

  @Autowired
  private ExceptionService exceptionService;

  /**
   * POST  /connections/add : Add a new connection.
   *
   * @param addDoctorDto the connection to add
   * @return the ResponseEntity with status 200 (OK) and update connection response message in body
   */
  @ApiOperation(value = "Add GMC connection", notes =
      "It will send add connection request to GMC and return GMC response message",
      response = String.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Add connection request has been sent to GMC successfully",
          response = String.class)})
  @PostMapping("/add")
  public ResponseEntity<UpdateConnectionResponseDto> addDoctor(
      @RequestBody final UpdateConnectionDto addDoctorDto) {
    log.info("Request receive to ADD doctor connection: {}", addDoctorDto);
    final var message = connectionService.addDoctor(addDoctorDto);
    return ResponseEntity.ok(message);
  }

  /**
   * POST  /connections/remove : Remove an existing connection.
   *
   * @param removeDoctorDto the connection to remove
   * @return the ResponseEntity with status 200 (OK) and update connection response message in body
   */
  @ApiOperation(value = "Remove GMC connection", notes =
      "It will send remove connection request to GMC and return GMC response message",
      response = String.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200,
          message = "Remove connection request has been sent to GMC successfully",
          response = String.class)})
  @PostMapping("/remove")
  public ResponseEntity<UpdateConnectionResponseDto> removeDoctor(
      @RequestBody final UpdateConnectionDto removeDoctorDto) {
    log.info("Request receive to REMOVE doctor connection: {}", removeDoctorDto);
    final var message = connectionService.removeDoctor(removeDoctorDto);
    return ResponseEntity.ok(message);
  }

  /**
   * POST  /connections/hide : Hide a connection.
   *
   * @param hideConnectionDto the connection to hide
   * @return the ResponseEntity with status 200 (OK) and update connection response message in body
   */
  @ApiOperation(value = "Hide connection", notes =
      "It will hide connections manually", response = String.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Hiding connections is successful",
          response = String.class)})
  @PostMapping("/hide")
  public ResponseEntity<UpdateConnectionResponseDto> hideConnection(
      @RequestBody final UpdateConnectionDto hideConnectionDto) {
    log.info("Request receive to hide doctor connection: {}", hideConnectionDto);
    final var message = connectionService.hideConnection(hideConnectionDto);
    return ResponseEntity.ok(message);
  }

  /**
   * POST  /connections/unhide : Unhide a connection.
   *
   * @param unhideConnectionDto the connection to unhide
   * @return the ResponseEntity with status 200 (OK) and update connection response message in body
   */
  @ApiOperation(value = "Unhide connection", notes =
      "It will unhide connections manually", response = String.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Unhiding connections is successful",
          response = String.class)})
  @PostMapping("/unhide")
  public ResponseEntity<UpdateConnectionResponseDto> unhideConnection(
      @RequestBody final UpdateConnectionDto unhideConnectionDto) {
    log.info("Request receive to unhide doctor connection: {}", unhideConnectionDto);
    final var message = connectionService.unhideConnection(unhideConnectionDto);
    return ResponseEntity.ok(message);
  }

  /**
   * GET  /connections/{gmcId} : get connection details of the gmcId.
   *
   * @param gmcId the connection details to get
   * @return the ResponseEntity with status 200 (OK) and connection details in body
   */
  @ApiOperation(value = "Get detailed connections of a trainee", notes =
      "It will return trainee's connections details", response = List.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Trainee connection details", response = List.class)})
  @GetMapping("/{gmcId}")
  public ResponseEntity<ConnectionDto> getDetailedConnections(
      @PathVariable("gmcId") final String gmcId) {
    log.info("Received request to fetch connections for GmcId: {}", gmcId);
    if (Objects.nonNull(gmcId)) {
      final var connections = connectionService.getTraineeConnectionInfo(gmcId);
      return ResponseEntity.ok().body(connections);
    }
    return ResponseEntity.ok().body((ConnectionDto) of());
  }

  /**
   * GET  /connections/hidden : get all gmcIds of hidden connections.
   *
   * @return the ResponseEntity with status 200(OK) and list of gmcIds of hidden connections in body
   */
  @ApiOperation(value = "Get detailed connections of a trainee", notes =
      "It will return trainee's connections details", response = List.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Trainee connection details", response = List.class)})
  @GetMapping("/hidden")
  public ResponseEntity<List<String>> getDetailedConnections() {
    log.info("Fetch all gmcIds of hidden connections");
    final var connections = connectionService.getAllHiddenConnections();
    return ResponseEntity.ok().body(connections);
  }

  /**
   * GET  /exception : get exception summary.
   *
   * @param sortColumn  column to be sorted
   * @param sortOrder   sorting order (ASC or DESC)
   * @param pageNumber  page number of data to get
   * @param dbcs        designated body code of the user
   * @param searchQuery search query of data to get
   * @return the ResponseEntity with status 200 (OK) and exception summary in body
   */
  @GetMapping("/exception")
  public ResponseEntity<ConnectionSummaryDto> getSummaryDiscrepancies(
      @RequestParam(name = SORT_COLUMN, defaultValue = GMC_REFERENCE_NUMBER,
          required = false) final String sortColumn,
      @RequestParam(name = SORT_ORDER, defaultValue = "desc",
          required = false) final String sortOrder,
      @RequestParam(name = PAGE_NUMBER, defaultValue = PAGE_NUMBER_VALUE,
          required = false) final int pageNumber,
      @RequestParam(name = DESIGNATED_BODY_CODES,
          required = false) final List<String> dbcs,
      @RequestParam(name = PROGRAMME_NAME,
          required = false) final String programmeName,
      @RequestParam(name = SEARCH_QUERY, defaultValue = EMPTY_STRING, required = false)
      String searchQuery) throws ConnectionQueryException {
    final var direction = "asc".equalsIgnoreCase(sortOrder) ? ASC : DESC;
    final var pageableAndSortable = of(pageNumber, 20,
        by(direction, ElasticsearchQueryHelper.formatSortFieldForElasticsearchQuery(sortColumn)));

    searchQuery = getConverter(searchQuery).fromJson().decodeUrl().escapeForSql().toString();
    var searchQueryES = getConverter(searchQuery).fromJson().decodeUrl().escapeForElasticSearch()
        .toString().toLowerCase();
    var connectionSummaryDto = discrepanciesElasticSearchService
        .searchForPage(searchQueryES, dbcs, programmeName, pageableAndSortable);

    return ResponseEntity.ok(connectionSummaryDto);

  }

  /**
   * GET  /connected : get connected summary.
   *
   * @param sortColumn  column to be sorted
   * @param sortOrder   sorting order (ASC or DESC)
   * @param pageNumber  page number of data to get
   * @param dbcs        designated body code of the user
   * @param searchQuery search query of data to get
   * @return the ResponseEntity with status 200 (OK) and connected summary in body
   */
  @GetMapping("/connected")
  public ResponseEntity<ConnectionSummaryDto> getSummaryConnected(
      @RequestParam(name = SORT_COLUMN, defaultValue = GMC_REFERENCE_NUMBER,
          required = false) final String sortColumn,
      @RequestParam(name = SORT_ORDER, defaultValue = "desc",
          required = false) final String sortOrder,
      @RequestParam(name = PAGE_NUMBER, defaultValue = PAGE_NUMBER_VALUE,
          required = false) final int pageNumber,
      @RequestParam(name = DESIGNATED_BODY_CODES,
          required = false) final List<String> dbcs,
      @RequestParam(name = PROGRAMME_NAME,
          required = false) final String programmeName,
      @RequestParam(name = SEARCH_QUERY, defaultValue = EMPTY_STRING, required = false)
      String searchQuery) throws ConnectionQueryException {
    final var direction = "asc".equalsIgnoreCase(sortOrder) ? ASC : DESC;
    final var pageableAndSortable = of(pageNumber, 20,
        by(direction, ElasticsearchQueryHelper.formatSortFieldForElasticsearchQuery(sortColumn)));

    searchQuery = getConverter(searchQuery).fromJson().decodeUrl().escapeForSql().toString();
    var searchQueryES = getConverter(searchQuery).fromJson().decodeUrl().escapeForElasticSearch()
        .toString().toLowerCase();
    var connectionSummaryDto = connectedElasticSearchService
        .searchForPage(searchQueryES, dbcs, programmeName, pageableAndSortable);

    return ResponseEntity.ok(connectionSummaryDto);
  }

  /**
   * GET  /disconnected : get disconnected summary.
   *
   * @param sortColumn  column to be sorted
   * @param sortOrder   sorting order (ASC or DESC)
   * @param pageNumber  page number of data to get
   * @param dbcs        designated body code of the user
   * @param searchQuery search query of data to get
   * @return the ResponseEntity with status 200 (OK) and connected summary in body
   */
  @GetMapping("/disconnected")
  public ResponseEntity<ConnectionSummaryDto> getSummaryDisconnected(
      @RequestParam(name = SORT_COLUMN, defaultValue = GMC_REFERENCE_NUMBER,
          required = false) final String sortColumn,
      @RequestParam(name = SORT_ORDER, defaultValue = "desc",
          required = false) final String sortOrder,
      @RequestParam(name = PAGE_NUMBER, defaultValue = PAGE_NUMBER_VALUE,
          required = false) final int pageNumber,
      @RequestParam(name = DESIGNATED_BODY_CODES,
          required = false) final List<String> dbcs,
      @RequestParam(name = SEARCH_QUERY, defaultValue = EMPTY_STRING, required = false)
      String searchQuery) {
    final var direction = "asc".equalsIgnoreCase(sortOrder) ? ASC : DESC;
    final var pageableAndSortable = of(pageNumber, 20,
        by(direction, sortColumn));

    searchQuery = getConverter(searchQuery).fromJson().decodeUrl().escapeForSql().toString();
    var searchQueryES = getConverter(searchQuery).fromJson().decodeUrl().escapeForElasticSearch()
        .toString().toLowerCase();
    var connectionSummaryDto = disconnectedElasticSearchService
        .searchForPage(searchQueryES, pageableAndSortable);

    return ResponseEntity.ok(connectionSummaryDto);
  }

  /**
   * GET  /exceptions/today : get list of exceptions.
   *
   * @return the list of ExceptionLogDto
   */
  @GetMapping("/exceptions/today")
  public ResponseEntity<List<ExceptionLogDto>> getListOfExceptions(
      @RequestParam(name = ADMIN) final String admin) {

    log.info("Received request to fetch exceptions for admin: {}", admin);
    final var exceptions = exceptionService.getExceptionLogs(admin);
    return ResponseEntity.ok().body(exceptions);

  }
}
