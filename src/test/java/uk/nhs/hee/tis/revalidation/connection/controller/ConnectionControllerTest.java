package uk.nhs.hee.tis.revalidation.connection.controller;

import static java.time.LocalDate.now;
import static java.util.List.of;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.data.domain.Sort.by;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionDto;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionHistoryDto;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionSummaryDto;
import uk.nhs.hee.tis.revalidation.connection.dto.DoctorInfoDto;
import uk.nhs.hee.tis.revalidation.connection.dto.UpdateConnectionDto;
import uk.nhs.hee.tis.revalidation.connection.dto.UpdateConnectionResponseDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestType;
import uk.nhs.hee.tis.revalidation.connection.service.ConnectedElasticSearchService;
import uk.nhs.hee.tis.revalidation.connection.service.ConnectionService;
import uk.nhs.hee.tis.revalidation.connection.service.DisconnectedElasticSearchService;
import uk.nhs.hee.tis.revalidation.connection.service.ExceptionElasticSearchService;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(ConnectionController.class)
class ConnectionControllerTest {

  private static final String SORT_COLUMN = "sortColumn";
  private static final String SORT_ORDER = "sortOrder";
  private static final String GMC_REFERENCE_NUMBER = "gmcReferenceNumber";
  private static final String PAGE_NUMBER = "pageNumber";
  private static final String PAGE_NUMBER_VALUE = "0";
  private static final String DESIGNATED_BODY_CODES = "dbcs";
  private static final String SEARCH_QUERY = "searchQuery";
  private static final String EMPTY_STRING = "";
  private final Faker faker = new Faker();
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockBean
  private ConnectionService connectionService;
  @MockBean
  private ExceptionElasticSearchService exceptionElasticSearchService;
  @MockBean
  private ConnectedElasticSearchService connectedElasticSearchService;
  @MockBean
  private DisconnectedElasticSearchService disconnectedElasticSearchService;
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
  private Long personId1;
  private Long personId2;
  private String gmcRef1;
  private String gmcRef2;
  private String firstName1;
  private String firstName2;
  private String lastName1;
  private String lastName2;
  private LocalDate submissionDate1;
  private LocalDate submissionDate2;
  private String designatedBody1;
  private String designatedBody2;
  private String programmeName1;
  private String programmeName2;
  private String programmeOwner1;
  private String programmeOwner2;

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

    personId1 = (long) faker.hashCode();
    personId2 = (long) faker.hashCode();
    gmcRef1 = faker.number().digits(8);
    gmcRef2 = faker.number().digits(8);
    firstName1 = faker.name().firstName();
    firstName2 = faker.name().firstName();
    lastName1 = faker.name().lastName();
    lastName2 = faker.name().lastName();
    submissionDate1 = now();
    submissionDate2 = now();
    designatedBody1 = faker.lorem().characters(8);
    designatedBody2 = faker.lorem().characters(8);
    programmeName1 = faker.lorem().characters(20);
    programmeName2 = faker.lorem().characters(20);
    programmeOwner1 = faker.lorem().characters(20);
    programmeOwner2 = faker.lorem().characters(20);
  }

  @Test
  void shouldAddDoctor() throws Exception {
    final var addDoctorDto = UpdateConnectionDto.builder().changeReason(changeReason)
        .designatedBodyCode(designatedBodyCode).doctors(buildDoctorsList()).build();

    final var response = UpdateConnectionResponseDto.builder().message(message).build();
    when(connectionService.addDoctor(any(UpdateConnectionDto.class))).thenReturn(response);
    this.mockMvc.perform(post("/api/connections/add")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(addDoctorDto)))
        .andExpect(content().json(objectMapper.writeValueAsString(response)))
        .andExpect(status().isOk());
  }

  @Test
  void shouldRemoveDoctor() throws Exception {
    final var removeDoctorDto = UpdateConnectionDto.builder().changeReason(changeReason)
        .designatedBodyCode(designatedBodyCode).doctors(buildDoctorsList()).build();

    final var response = UpdateConnectionResponseDto.builder().message(message).build();
    when(connectionService.removeDoctor(any(UpdateConnectionDto.class))).thenReturn(response);
    this.mockMvc.perform(post("/api/connections/remove")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(removeDoctorDto)))
        .andExpect(content().json(objectMapper.writeValueAsString(response)))
        .andExpect(status().isOk());
  }

  @Test
  void shouldHideDoctorConnection() throws Exception {
    final var hideDoctorDto = UpdateConnectionDto.builder().changeReason(changeReason)
        .doctors(buildDoctorsList()).build();
    final var response = UpdateConnectionResponseDto.builder().message(message).build();
    when(connectionService.hideConnection(any(UpdateConnectionDto.class))).thenReturn(response);
    this.mockMvc.perform(post("/api/connections/hide")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(hideDoctorDto)))
        .andExpect(content().json(objectMapper.writeValueAsString(response)))
        .andExpect(status().isOk());
  }

  @Test
  void shouldUnhideDoctorConnection() throws Exception {
    final var unhideDoctorDto = UpdateConnectionDto.builder().changeReason(changeReason)
        .doctors(buildDoctorsList()).build();
    final var response = UpdateConnectionResponseDto.builder().message(message).build();
    when(connectionService.unhideConnection(any(UpdateConnectionDto.class))).thenReturn(response);
    this.mockMvc.perform(post("/api/connections/unhide")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(unhideDoctorDto)))
        .andExpect(content().json(objectMapper.writeValueAsString(response)))
        .andExpect(status().isOk());
  }

  @Test
  void shouldReturnAllConnectionsForADoctor() throws Exception {
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
  void shouldNotFailWhenThereIsNoConnectionsForADoctor() throws Exception {
    when(connectionService.getTraineeConnectionInfo(gmcId)).thenReturn(new ConnectionDto());
    this.mockMvc.perform(get("/api/connections/{gmcId}", gmcId))
        .andExpect(status().isOk());
  }

  @Test
  void shouldReturnAllHiddenConnections() throws Exception {
    final var gmcIds = List.of(gmcId);
    when(connectionService.getAllHiddenConnections()).thenReturn(gmcIds);
    this.mockMvc.perform(get("/api/connections/hidden"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.[*]").value(hasItem(gmcId)));
  }

  @Test
  void shouldNotFailWhenThereIsNoHiddenConnections() throws Exception {
    when(connectionService.getAllHiddenConnections()).thenReturn(List.of());
    this.mockMvc.perform(get("/api/connections/hidden"))
        .andExpect(status().isOk());
  }

  @Test
  void shouldReturnExceptionTraineeDoctorsInformation() throws Exception {
    final var connectionSummary = prepareConnectionSummary();
    final var pageableAndSortable = PageRequest.of(Integer.parseInt(PAGE_NUMBER_VALUE), 20,
        by(ASC, "gmcReferenceNumber.keyword"));
    when(exceptionElasticSearchService.searchForPage(EMPTY_STRING, pageableAndSortable))
        .thenReturn(connectionSummary);
    final var dbcString = String.format("%s,%s", designatedBody1, designatedBody2);
    this.mockMvc.perform(get("/api/connections/exception")
        .param(SORT_ORDER, "asc")
        .param(SORT_COLUMN, GMC_REFERENCE_NUMBER)
        .param(PAGE_NUMBER, PAGE_NUMBER_VALUE)
        .param(SEARCH_QUERY, EMPTY_STRING)
        .param(DESIGNATED_BODY_CODES, dbcString))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.connections.[*].tcsPersonId").value(hasItem(personId1.intValue())))
        .andExpect(
            jsonPath("$.connections.[*].gmcReferenceNumber").value(hasItem(gmcRef2)))
        .andExpect(
            jsonPath("$.connections.[*].doctorFirstName").value(hasItem(firstName2)))
        .andExpect(
            jsonPath("$.connections.[*].doctorLastName").value(hasItem(lastName2)))
        .andExpect(
            jsonPath("$.connections.[*].programmeName").value(hasItem(programmeName2)))
        .andExpect(
            jsonPath("$.connections.[*].designatedBody").value(hasItem(designatedBody2)))
        .andExpect(
            jsonPath("$.connections.[*].programmeOwner").value(hasItem(programmeOwner2)));
  }

  @Test
  void shouldReturnExceptionTraineeDoctorsInformationDesc() throws Exception {
    final var connectionSummary = prepareConnectionSummary();
    final var pageableAndSortable = PageRequest.of(Integer.parseInt(PAGE_NUMBER_VALUE), 20,
        by(DESC, "gmcReferenceNumber.keyword"));
    when(exceptionElasticSearchService.searchForPage(EMPTY_STRING, pageableAndSortable))
        .thenReturn(connectionSummary);
    final var dbcString = String.format("%s,%s", designatedBody1, designatedBody2);
    this.mockMvc.perform(get("/api/connections/exception")
        .param(SORT_ORDER, "desc")
        .param(SORT_COLUMN, GMC_REFERENCE_NUMBER)
        .param(PAGE_NUMBER, PAGE_NUMBER_VALUE)
        .param(SEARCH_QUERY, EMPTY_STRING)
        .param(DESIGNATED_BODY_CODES, dbcString))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.connections.[*].tcsPersonId").value(hasItem(personId1.intValue())));
  }

  @Test
  void shouldReturnConnectedTraineeDoctorsInformation() throws Exception {
    final var connectionSummary = prepareConnectionSummary();
    final var pageableAndSortable = PageRequest.of(Integer.parseInt(PAGE_NUMBER_VALUE), 20,
        by(ASC, "gmcReferenceNumber.keyword"));
    when(connectedElasticSearchService.searchForPage(EMPTY_STRING, pageableAndSortable))
        .thenReturn(connectionSummary);
    final var dbcString = String.format("%s,%s", designatedBody1, designatedBody2);
    this.mockMvc.perform(get("/api/connections/connected")
        .param(SORT_ORDER, "asc")
        .param(SORT_COLUMN, GMC_REFERENCE_NUMBER)
        .param(PAGE_NUMBER, PAGE_NUMBER_VALUE)
        .param(SEARCH_QUERY, EMPTY_STRING)
        .param(DESIGNATED_BODY_CODES, dbcString))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.connections.[*].tcsPersonId").value(hasItem(personId1.intValue())))
        .andExpect(
            jsonPath("$.connections.[*].gmcReferenceNumber").value(hasItem(gmcRef2)))
        .andExpect(
            jsonPath("$.connections.[*].doctorFirstName").value(hasItem(firstName2)))
        .andExpect(
            jsonPath("$.connections.[*].doctorLastName").value(hasItem(lastName2)))
        .andExpect(
            jsonPath("$.connections.[*].programmeName").value(hasItem(programmeName2)))
        .andExpect(
            jsonPath("$.connections.[*].designatedBody").value(hasItem(designatedBody2)))
        .andExpect(
            jsonPath("$.connections.[*].programmeOwner").value(hasItem(programmeOwner2)));
  }

  @Test
  void shouldReturnConnectedTraineeDoctorsInformationDesc() throws Exception {
    final var connectionSummary = prepareConnectionSummary();
    final var pageableAndSortable = PageRequest.of(Integer.parseInt(PAGE_NUMBER_VALUE), 20,
        by(DESC, "gmcReferenceNumber.keyword"));
    when(connectedElasticSearchService.searchForPage(EMPTY_STRING, pageableAndSortable))
        .thenReturn(connectionSummary);
    final var dbcString = String.format("%s,%s", designatedBody1, designatedBody2);
    this.mockMvc.perform(get("/api/connections/connected")
        .param(SORT_ORDER, "desc")
        .param(SORT_COLUMN, GMC_REFERENCE_NUMBER)
        .param(PAGE_NUMBER, PAGE_NUMBER_VALUE)
        .param(SEARCH_QUERY, EMPTY_STRING)
        .param(DESIGNATED_BODY_CODES, dbcString))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.connections.[*].tcsPersonId").value(hasItem(personId1.intValue())));
  }

  @Test
  void shouldReturnDisconnectedTraineeDoctorsInformation() throws Exception {
    final var connectionSummary = prepareConnectionSummary();
    final var pageableAndSortable = PageRequest.of(Integer.parseInt(PAGE_NUMBER_VALUE), 20,
        by(ASC, "gmcReferenceNumber.keyword"));
    when(disconnectedElasticSearchService.searchForPage(EMPTY_STRING, pageableAndSortable))
        .thenReturn(connectionSummary);
    final var dbcString = String.format("%s,%s", designatedBody1, designatedBody2);
    this.mockMvc.perform(get("/api/connections/disconnected")
        .param(SORT_ORDER, "asc")
        .param(SORT_COLUMN, GMC_REFERENCE_NUMBER)
        .param(PAGE_NUMBER, PAGE_NUMBER_VALUE)
        .param(SEARCH_QUERY, EMPTY_STRING)
        .param(DESIGNATED_BODY_CODES, dbcString))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.connections.[*].tcsPersonId").value(hasItem(personId1.intValue())))
        .andExpect(
            jsonPath("$.connections.[*].gmcReferenceNumber").value(hasItem(gmcRef2)))
        .andExpect(
            jsonPath("$.connections.[*].doctorFirstName").value(hasItem(firstName2)))
        .andExpect(
            jsonPath("$.connections.[*].doctorLastName").value(hasItem(lastName2)))
        .andExpect(
            jsonPath("$.connections.[*].programmeName").value(hasItem(programmeName2)))
        .andExpect(
            jsonPath("$.connections.[*].designatedBody").value(hasItem(designatedBody2)))
        .andExpect(
            jsonPath("$.connections.[*].programmeOwner").value(hasItem(programmeOwner2)));
  }

  @Test
  void shouldReturnDisconnectedTraineeDoctorsInformationDesc() throws Exception {
    final var connectionSummary = prepareConnectionSummary();
    final var pageableAndSortable = PageRequest.of(Integer.parseInt(PAGE_NUMBER_VALUE), 20,
        by(DESC, "gmcReferenceNumber.keyword"));
    when(disconnectedElasticSearchService.searchForPage(EMPTY_STRING, pageableAndSortable))
        .thenReturn(connectionSummary);
    final var dbcString = String.format("%s,%s", designatedBody1, designatedBody2);
    this.mockMvc.perform(get("/api/connections/disconnected")
        .param(SORT_ORDER, "desc")
        .param(SORT_COLUMN, GMC_REFERENCE_NUMBER)
        .param(PAGE_NUMBER, PAGE_NUMBER_VALUE)
        .param(SEARCH_QUERY, EMPTY_STRING)
        .param(DESIGNATED_BODY_CODES, dbcString))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.connections.[*].tcsPersonId").value(hasItem(personId1.intValue())));
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

  private ConnectionSummaryDto prepareConnectionSummary() {
    final var doctorsForDB = buildDoctorsForDBList();
    return ConnectionSummaryDto.builder()
        .connections(doctorsForDB)
        .totalResults(doctorsForDB.size())
        .build();
  }

  private List<ConnectionInfoDto> buildDoctorsForDBList() {
    final var doctor1 = ConnectionInfoDto.builder()
        .tcsPersonId(personId1)
        .gmcReferenceNumber(gmcRef1)
        .doctorFirstName(firstName1)
        .doctorLastName(lastName1)
        .submissionDate(submissionDate1)
        .programmeName(programmeName1)
        .designatedBody(designatedBody1)
        .programmeOwner(programmeOwner1)
        .build();

    final var doctor2 = ConnectionInfoDto.builder()
        .tcsPersonId(personId2)
        .gmcReferenceNumber(gmcRef2)
        .doctorFirstName(firstName2)
        .doctorLastName(lastName2)
        .submissionDate(submissionDate2)
        .programmeName(programmeName2)
        .designatedBody(designatedBody2)
        .programmeOwner(programmeOwner2)
        .build();
    return of(doctor1, doctor2);
  }
}
