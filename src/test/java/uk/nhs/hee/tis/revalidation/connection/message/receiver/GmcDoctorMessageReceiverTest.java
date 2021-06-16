package uk.nhs.hee.tis.revalidation.connection.message.receiver;

import static java.time.LocalDate.now;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.GmcDoctor;
import uk.nhs.hee.tis.revalidation.connection.entity.MasterDoctorView;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapper;
import uk.nhs.hee.tis.revalidation.connection.mapper.ConnectionInfoMapperImpl;
import uk.nhs.hee.tis.revalidation.connection.repository.MasterElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.connection.service.ConnectionService;
import uk.nhs.hee.tis.revalidation.connection.service.ElasticSearchIndexUpdateHelper;
import uk.nhs.hee.tis.revalidation.connection.service.MasterElasticSearchService;

@ExtendWith(MockitoExtension.class)
public class GmcDoctorMessageReceiverTest {

  Faker faker = new Faker();

  GmcDoctorMessageReceiver gmcDoctorMessageReceiver;
  @Mock
  ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper;
  @Mock
  MasterElasticSearchService masterElasticSearchService;
  @Mock
  MasterElasticSearchRepository masterElasticSearchRepository;
  ConnectionInfoMapper connectionInfoMapper;

  private GmcDoctor gmcDoctor;
  private ConnectionService connectionService;
  private String gmcRef1;
  private String firstName1;
  private String lastName1;
  private LocalDate submissionDate1;
  private String designatedBody1;
  private String programmeName1;
  private String programmeOwner1;
  private String status;
  ConnectionInfoDto connectionInfoDto;
  private List<ConnectionInfoDto> connectionInfoDtos = new ArrayList<>();
  private List<MasterDoctorView> masterDoctorViews = new ArrayList<>();

  @BeforeEach
  public void setup() {
    connectionInfoMapper = new ConnectionInfoMapperImpl();

    gmcDoctorMessageReceiver = new GmcDoctorMessageReceiver(
        elasticSearchIndexUpdateHelper,
        masterElasticSearchService,
        masterElasticSearchRepository,
        connectionInfoMapper
    );
    gmcRef1 = faker.number().digits(8);
    firstName1 = faker.name().firstName();
    lastName1 = faker.name().lastName();
    submissionDate1 = now();
    designatedBody1 = faker.lorem().characters(8);
    programmeName1 = faker.lorem().characters(20);
    programmeOwner1 = faker.lorem().characters(20);
    status = faker.lorem().characters(8);

    connectionInfoDto = buildConnectionInfoDto();
    connectionInfoDtos.add(connectionInfoDto);
    masterDoctorViews.add(connectionInfoMapper.dtoToMaster(connectionInfoDto));

    gmcDoctor = buildGmcDoctor();
  }

  @Test
  void shouldUpdateConnectionsOnReceiveMessage() {
    when(masterElasticSearchRepository.findByGmcReferenceNumber(gmcRef1)).thenReturn(masterDoctorViews);
    gmcDoctorMessageReceiver.handleMessage(gmcDoctor);
    verify(elasticSearchIndexUpdateHelper).updateElasticSearchIndex(connectionInfoDto);
  }

  @Test
  void shouldUpdateMasterOnReceiveMessage() {
    when(masterElasticSearchRepository.findByGmcReferenceNumber(gmcRef1)).thenReturn(masterDoctorViews);
    gmcDoctorMessageReceiver.handleMessage(gmcDoctor);
    verify(masterElasticSearchService).updateMasterIndex(connectionInfoDto);
  }

  private GmcDoctor buildGmcDoctor() {
    return GmcDoctor.builder()
        .gmcReferenceNumber(gmcRef1)
        .doctorFirstName(firstName1)
        .doctorLastName(lastName1)
        .submissionDate(submissionDate1.toString())
        .doctorStatus(status)
        .breach(faker.lorem().characters(20))
        .dateAdded(faker.date().past(10, TimeUnit.DAYS).toString())
        .underNotice(faker.lorem().characters(5))
        .investigation(faker.lorem().characters(20))
        .preliminaryInvestigation(faker.lorem().characters(20))
        .sanction(faker.lorem().characters(10))
        .designatedBodyCode(designatedBody1)
        .build();
  }

  private ConnectionInfoDto buildConnectionInfoDto() {
    return ConnectionInfoDto.builder()
        .tcsPersonId((long) 111)
        .gmcReferenceNumber(gmcRef1)
        .doctorFirstName(firstName1)
        .doctorLastName(lastName1)
        .submissionDate(submissionDate1)
        .programmeName(programmeName1)
        .designatedBody(designatedBody1)
        .programmeOwner(programmeOwner1)
        .connectionStatus(status)
        .build();
  }

}
