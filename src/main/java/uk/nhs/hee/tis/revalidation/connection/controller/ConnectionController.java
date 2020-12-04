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
import uk.nhs.hee.tis.revalidation.connection.dto.AddRemoveDoctorDto;
import uk.nhs.hee.tis.revalidation.connection.dto.AddRemoveResponseDto;
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
  public ResponseEntity<AddRemoveResponseDto> addDoctor(
      @RequestBody final AddRemoveDoctorDto addDoctorDto) {
    log.info("Request receive to ADD doctor connection: {}", addDoctorDto);
    final var message = connectionService.addDoctor(addDoctorDto);
    return ResponseEntity.ok(message);
  }

  @ApiOperation(value = "Remove GMC connection", notes =
      "It will send remove connection request to GMC and return GMC response message", response = String.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Remove connection request has been sent to GMC successfully", response = String.class)})
  @PostMapping("/remove")
  public ResponseEntity<AddRemoveResponseDto> removeDoctor(
      @RequestBody final AddRemoveDoctorDto removeDoctorDto) {
    log.info("Request receive to REMOVE doctor connection: {}", removeDoctorDto);
    final var message = connectionService.removeDoctor(removeDoctorDto);
    return ResponseEntity.ok(message);
  }

  @ApiOperation(value = "Get detailed connections of a trainee", notes =
      "It will return trainee's connections details", response = List.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Trainee connection details", response = List.class)})
  @GetMapping("/{gmcId}")
  public ResponseEntity<List<ConnectionDto>> getDetailedConnections(
      @PathVariable("gmcId") final String gmcId) {
    log.info("Received request to fetch connections for GmcId: {}", gmcId);
    if (Objects.nonNull(gmcId)) {
      final var connections = connectionService.getTraineeConnectionInfo(gmcId);
      return ResponseEntity.ok().body(connections);
    }
    return ResponseEntity.ok().body(of());
  }
}
