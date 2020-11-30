package uk.nhs.hee.tis.revalidation.connection.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
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
import uk.nhs.hee.tis.revalidation.connection.dto.DoctorInfoDto;
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

  @BeforeEach
  public void setup() {
    changeReason = faker.lorem().sentence();
    designatedBodyCode = faker.number().digits(8);
    gmcId = faker.number().digits(8);
    message = faker.lorem().sentence();
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

  private List<DoctorInfoDto> buildDoctorsList() {
    final var doc1 = DoctorInfoDto.builder().gmcId(gmcId)
        .currentDesignatedBodyCode(designatedBodyCode).build();
    final var doc2 = DoctorInfoDto.builder().gmcId(gmcId)
        .currentDesignatedBodyCode(designatedBodyCode).build();
    return List.of(doc1, doc2);
  }
}
