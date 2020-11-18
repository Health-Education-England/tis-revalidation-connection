package uk.nhs.hee.tis.revalidation.connection.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
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
    final var addDoctorDto = AddRemoveDoctorDto.builder().gmcId(gmcId).changeReason(changeReason)
        .designatedBodyCode(designatedBodyCode).build();

    when(connectionService.addDoctor(any(AddRemoveDoctorDto.class))).thenReturn(message);
    this.mockMvc.perform(post("/api/connections/add")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(addDoctorDto)))
        .andExpect(content().string(message))
        .andExpect(status().isOk());
  }

  @Test
  public void shouldRemoveDoctor() throws Exception {
    final var removeDoctorDto = AddRemoveDoctorDto.builder().gmcId(gmcId).changeReason(changeReason)
        .designatedBodyCode(designatedBodyCode).build();

    when(connectionService.removeDoctor(any(AddRemoveDoctorDto.class))).thenReturn(message);
    this.mockMvc.perform(post("/api/connections/remove")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(removeDoctorDto)))
        .andExpect(content().string(message))
        .andExpect(status().isOk());
  }
}
