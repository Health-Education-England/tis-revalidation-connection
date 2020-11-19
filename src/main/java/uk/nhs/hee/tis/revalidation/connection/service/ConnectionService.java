package uk.nhs.hee.tis.revalidation.connection.service;

import static java.time.LocalDateTime.now;
import static uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestType.ADD;
import static uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestType.REMOVE;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.dto.AddRemoveDoctorDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestLog;
import uk.nhs.hee.tis.revalidation.connection.entity.GmcResponseCode;
import uk.nhs.hee.tis.revalidation.connection.repository.ConnectionRepository;

@Slf4j
@Service
public class ConnectionService {

  @Autowired
  private GmcClientService gmcClientService;

  @Autowired
  private ConnectionRepository repository;

  @Autowired
  private RabbitTemplate rabbitTemplate;

  @Value("${app.rabbit.exchange}")
  private String exchange;

  @Value("${app.rabbit.routingkey}")
  private String routingKey;

  public String addDoctor(final AddRemoveDoctorDto addDoctorDto) {
    final var gmcResponse = gmcClientService.addDoctor(addDoctorDto);
    final var connectionRequestLog = ConnectionRequestLog.builder()
        .id(UUID.randomUUID().toString())
        .gmcId(addDoctorDto.getGmcId())
        .gmcClientId(gmcResponse.getGmcRequestId())
        .reason(addDoctorDto.getChangeReason())
        .requestType(ADD)
        .responseCode(gmcResponse.getReturnCode())
        .requestTime(now())
        .build();

    repository.save(connectionRequestLog);
    final var gmcResponseCode = GmcResponseCode.fromCode(gmcResponse.getReturnCode());
    return gmcResponseCode != null ? gmcResponseCode.getMessage() : "";

  }

  public String removeDoctor(final AddRemoveDoctorDto removeDoctorDto) {

    final var gmcResponse = gmcClientService.removeDoctor(removeDoctorDto);
    final var connectionRequestLog = ConnectionRequestLog.builder()
        .id(UUID.randomUUID().toString())
        .gmcId(removeDoctorDto.getGmcId())
        .gmcClientId(gmcResponse.getGmcRequestId())
        .reason(removeDoctorDto.getChangeReason())
        .requestType(REMOVE)
        .responseCode(gmcResponse.getReturnCode())
        .requestTime(now())
        .build();

    repository.save(connectionRequestLog);
    sendToRabbit(removeDoctorDto.getGmcId(), gmcResponse.getReturnCode());
    final var gmcResponseCode = GmcResponseCode.fromCode(gmcResponse.getReturnCode());
    return gmcResponseCode != null ? gmcResponseCode.getMessage() : "";

  }

  private void sendToRabbit(final String gmcId, final String returnCode) {
    if (GmcResponseCode.SUCCESS.getCode().equals(returnCode)) {
      rabbitTemplate.convertAndSend(exchange, routingKey, gmcId);
    }
  }
}
