package uk.nhs.hee.tis.revalidation.connection.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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

  @PostMapping("/add")
  public ResponseEntity<String> addDoctor(final AddRemoveDoctorDto addDoctorDto) {
    log.info("Request receive to ADD doctor connection: {}", addDoctorDto);
    final var message = connectionService.addDoctor(addDoctorDto);
    return ResponseEntity.ok(message);
  }

  @PostMapping("/remove")
  public ResponseEntity<String> removeDoctor(final AddRemoveDoctorDto removeDoctorDto) {
    log.info("Request receive to REMOVE doctor connection: {}", removeDoctorDto);
    final var message = connectionService.removeDoctor(removeDoctorDto);
    return ResponseEntity.ok(message);
  }
}
