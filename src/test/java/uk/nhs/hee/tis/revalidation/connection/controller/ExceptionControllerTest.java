package uk.nhs.hee.tis.revalidation.connection.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.nhs.hee.tis.revalidation.connection.service.ExceptionService;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(ExceptionController.class)
class ExceptionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ExceptionService exceptionService;

  @Test
  void shouldGetExceptions() throws Exception {
    this.mockMvc.perform(get("/api/exception"))
        .andExpect(status().isOk());
  }


}
