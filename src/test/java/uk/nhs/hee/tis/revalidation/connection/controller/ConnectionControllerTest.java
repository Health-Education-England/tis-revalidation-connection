package uk.nhs.hee.tis.revalidation.connection.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.nhs.hee.tis.revalidation.connection.dto.AddRemoveDoctorDto;
import uk.nhs.hee.tis.revalidation.connection.dto.AddRemoveResponseDto;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionDto;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionHistoryDto;
import uk.nhs.hee.tis.revalidation.connection.dto.DoctorInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestType;
import uk.nhs.hee.tis.revalidation.connection.service.ConnectionService;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(ConnectionController.class)
class ConnectionControllerTest {

  private final Faker faker = new Faker();

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ConnectionService connectionService;

  private String changeReason;
  private String designatedBodyCode;
  private String gmcId;
  private String message;

  private String connectionId;
  private String gmcClientId;
  private String newDesignatedBodyCode;
  private String previousDesignatedBodyCode;
  private String reason;
  private String reasonMessage;
  private ConnectionRequestType requestType;
  private LocalDateTime requestTime;
  private String responseCode;

  @BeforeEach
  public void setup() {
    changeReason = faker.lorem().sentence();
    designatedBodyCode = faker.number().digits(8);
    gmcId = faker.number().digits(8);
    message = faker.lorem().sentence();

    connectionId = faker.number().digits(20);
    gmcClientId = faker.number().digits(8);
    newDesignatedBodyCode = faker.number().digits(8);
    previousDesignatedBodyCode = faker.number().digits(8);
    reason = "2";
    reasonMessage = "Conflict of Interest";
    requestType = ConnectionRequestType.ADD;
    requestTime = LocalDateTime.now().minusDays(-1);
    responseCode = faker.number().digits(5);
  }

  @Test
  public void shouldAddDoctor() throws Exception {
    final var addDoctorDto = AddRemoveDoctorDto.builder().changeReason(changeReason)
        .designatedBodyCode(designatedBodyCode).doctors(buildDoctorsList()).build();

    final var response = AddRemoveResponseDto.builder().message(message).build();
    when(connectionService.addDoctor(any(AddRemoveDoctorDto.class))).thenReturn(response);
    this.mockMvc.perform(post("/api/connections/add")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(addDoctorDto)))
        .andExpect(content().json(objectMapper.writeValueAsString(response)))
        .andExpect(status().isOk());
  }

  @Test
  public void shouldRemoveDoctor() throws Exception {
    final var removeDoctorDto = AddRemoveDoctorDto.builder().changeReason(changeReason)
        .designatedBodyCode(designatedBodyCode).doctors(buildDoctorsList()).build();

    final var response = AddRemoveResponseDto.builder().message(message).build();
    when(connectionService.removeDoctor(any(AddRemoveDoctorDto.class))).thenReturn(response);
    this.mockMvc.perform(post("/api/connections/remove")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(removeDoctorDto)))
        .andExpect(content().json(objectMapper.writeValueAsString(response)))
        .andExpect(status().isOk());
  }

  @Test
  public void shouldReturnAllConnectionsForADoctor() throws Exception {
    final var connectionDto = prepareConnectionDto();
    when(connectionService.getTraineeConnectionInfo(gmcId)).thenReturn(connectionDto);
    this.mockMvc.perform(get("/api/connections/{gmcId}", gmcId))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(connectionDto)))
        .andExpect(
            jsonPath("$.connectionHistory.[*].connectionId").value(hasItem(connectionId)))
        .andExpect(jsonPath("$.connectionHistory.[*].gmcId").value(hasItem(gmcId)))
        .andExpect(jsonPath("$.connectionHistory.[*].newDesignatedBodyCode")
            .value(hasItem(newDesignatedBodyCode)))
        .andExpect(jsonPath("$.connectionHistory.[*].previousDesignatedBodyCode")
            .value(hasItem(previousDesignatedBodyCode)))
        .andExpect(jsonPath("$.connectionHistory.[*].reason").value(hasItem(reason)))
        .andExpect(jsonPath("$.connectionHistory.[*].requestType")
            .value(hasItem(requestType.toString())))
        .andExpect(
            jsonPath("$.connectionHistory.[*].responseCode").value(hasItem(responseCode)));
  }

  @Test
  public void shouldNotFailWhenThereIsNoConnectionsForADoctor() throws Exception {
    when(connectionService.getTraineeConnectionInfo(gmcId)).thenReturn(new ConnectionDto());
    this.mockMvc.perform(get("/api/connections/{gmcId}", gmcId))
        .andExpect(status().isOk());
  }

  private ConnectionDto prepareConnectionDto() {
    final ConnectionDto connectionDto = new ConnectionDto();
    final ConnectionHistoryDto connectionHistory = ConnectionHistoryDto.builder()
        .connectionId(connectionId)
        .gmcId(gmcId)
        .gmcClientId(gmcClientId)
        .newDesignatedBodyCode(newDesignatedBodyCode)
        .previousDesignatedBodyCode(previousDesignatedBodyCode)
        .reason(reason)
        .requestType(requestType)
        .requestTime(requestTime)
        .responseCode(responseCode)
        .build();
    connectionDto.setConnectionHistory(List.of(connectionHistory));
    return connectionDto;
  }

  private List<DoctorInfoDto> buildDoctorsList() {
    final var doc1 = DoctorInfoDto.builder().gmcId(gmcId)
        .currentDesignatedBodyCode(designatedBodyCode).build();
    final var doc2 = DoctorInfoDto.builder().gmcId(gmcId)
        .currentDesignatedBodyCode(designatedBodyCode).build();
    return List.of(doc1, doc2);
  }
}
