package uk.nhs.hee.tis.revalidation.connection.controller;

import static java.util.List.of;

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
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.tis.revalidation.connection.dto.UpdateConnectionDto;
import uk.nhs.hee.tis.revalidation.connection.dto.UpdateConnectionResponseDto;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionDto;
import uk.nhs.hee.tis.revalidation.connection.service.ConnectionService;

@Slf4j
@RestController
@RequestMapping("/api/connections")
public class ConnectionController {

  @Autowired
  private ConnectionService connectionService;


  @ApiOperation(value = "Add GMC connection", notes =
      "It will send add connection request to GMC and return GMC response message", response = String.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Add connection request has been sent to GMC successfully", response = String.class)})
  @PostMapping("/add")
  public ResponseEntity<UpdateConnectionResponseDto> addDoctor(
      @RequestBody final UpdateConnectionDto addDoctorDto) {
    log.info("Request receive to ADD doctor connection: {}", addDoctorDto);
    final var message = connectionService.addDoctor(addDoctorDto);
    return ResponseEntity.ok(message);
  }

  @ApiOperation(value = "Remove GMC connection", notes =
      "It will send remove connection request to GMC and return GMC response message", response = String.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Remove connection request has been sent to GMC successfully", response = String.class)})
  @PostMapping("/remove")
  public ResponseEntity<UpdateConnectionResponseDto> removeDoctor(
      @RequestBody final UpdateConnectionDto removeDoctorDto) {
    log.info("Request receive to REMOVE doctor connection: {}", removeDoctorDto);
    final var message = connectionService.removeDoctor(removeDoctorDto);
    return ResponseEntity.ok(message);
  }

  @ApiOperation(value = "Hide connection", notes =
      "It will hide connections manually", response = String.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Hiding connections is successful", response = String.class)})
  @PostMapping("/hide")
  public ResponseEntity<UpdateConnectionResponseDto> hideConnection(
      @RequestBody final UpdateConnectionDto hideConnectionDto) {
    log.info("Request receive to hide doctor connection: {}", hideConnectionDto);
    final var message = connectionService.hideConnection(hideConnectionDto);
    return ResponseEntity.ok(message);
  }

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
}
