package uk.nhs.hee.tis.revalidation.connection.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.tis.revalidation.connection.dto.AddRemoveDoctorDto;
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
  public ResponseEntity<String> addDoctor(@RequestBody final AddRemoveDoctorDto addDoctorDto) {
    log.info("Request receive to ADD doctor connection: {}", addDoctorDto);
    final var message = connectionService.addDoctor(addDoctorDto);
    return ResponseEntity.ok(message);
  }

  @ApiOperation(value = "Remove GMC connection", notes =
      "It will send remove connection request to GMC and return GMC response message", response = String.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Remove connection request has been sent to GMC successfully", response = String.class)})
  @PostMapping("/remove")
  public ResponseEntity<String> removeDoctor(@RequestBody final AddRemoveDoctorDto removeDoctorDto) {
    log.info("Request receive to REMOVE doctor connection: {}", removeDoctorDto);
    final var message = connectionService.removeDoctor(removeDoctorDto);
    return ResponseEntity.ok(message);
  }
}
