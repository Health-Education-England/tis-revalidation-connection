package uk.nhs.hee.tis.revalidation.connection.message.receiver;

import static java.time.LocalDate.now;
import static org.mockito.Mockito.verify;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.service.ConnectionService;
import uk.nhs.hee.tis.revalidation.connection.service.ElasticSearchIndexUpdateHelper;
import uk.nhs.hee.tis.revalidation.connection.service.MasterElasticSearchService;

@ExtendWith(MockitoExtension.class)
public class ConnectionMessageReceiverTest {
  @InjectMocks
  private ConnectionMessageReceiver connectionMessageReceiver;
  @Mock
  private ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper;
  @Mock
  private MasterElasticSearchService masterElasticSearchService;
  @Mock
  private ConnectionService connectionService;
  private Faker faker = new Faker();
  private String gmcRef1;
  private String firstName1;
  private String lastName1;
  private LocalDate submissionDate1;
  private String designatedBody1;
  private String programmeName1;
  private String programmeOwner1;
  ConnectionInfoDto connectionInfoDto;
  private List<ConnectionInfoDto> connectionInfoDtos = new ArrayList<>();

  /**
   * Set up data for testing.
   */
  @BeforeEach
  public void setup() {
    firstName1 = faker.name().firstName();
    lastName1 = faker.name().lastName();
    submissionDate1 = now();
    designatedBody1 = faker.lorem().characters(8);
    programmeName1 = faker.lorem().characters(20);
    programmeOwner1 = faker.lorem().characters(20);

    connectionInfoDto = ConnectionInfoDto.builder()
        .tcsPersonId((long) 111)
        .gmcReferenceNumber(null) //TODO: replace call to mongo
        .doctorFirstName(firstName1)
        .doctorLastName(lastName1)
        .submissionDate(submissionDate1)
        .programmeName(programmeName1)
        .designatedBody(designatedBody1)
        .programmeOwner(programmeOwner1)
        .build();
    connectionInfoDtos.add(connectionInfoDto);
  }

  @Test
  void shouldUpdateMasterIndexAndOtherIndexesOnReceiveConnectionInfo() {
    connectionMessageReceiver.handleMessage(connectionInfoDto);
    verify(masterElasticSearchService).updateMasterIndex(connectionInfoDto);
    verify(elasticSearchIndexUpdateHelper).updateElasticSearchIndex(connectionInfoDto);
  }
}
