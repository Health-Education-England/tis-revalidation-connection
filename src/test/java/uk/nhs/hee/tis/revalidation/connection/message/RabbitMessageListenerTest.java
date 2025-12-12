package uk.nhs.hee.tis.revalidation.connection.message;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.github.javafaker.Faker;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionLogDto;
import uk.nhs.hee.tis.revalidation.connection.service.ConnectionService;

@ExtendWith(MockitoExtension.class)
class RabbitMessageListenerTest {

  private static final Faker faker = Faker.instance();

  private static final String GMC_ID = faker.lorem().characters(8);
  private static final String NEW_DBC = faker.lorem().characters(8);
  private static final String OLD_DBC = faker.lorem().characters(8);
  private static final String UPDATED_BY = faker.lorem().characters(8);
  private static final LocalDateTime EVENT_DATETIME = LocalDateTime.now();

  @InjectMocks
  RabbitMessageListener rabbitMessageListener;

  @Mock
  ConnectionService connectionService;

  @Captor
  ArgumentCaptor<ConnectionLogDto> connectionLogDtoArgumentCaptor;

  @Test
  void shouldHandleConnectionLogMessage() {
    ConnectionLogDto connectionLogDto = ConnectionLogDto.builder().gmcId(GMC_ID)
        .newDesignatedBodyCode(NEW_DBC).previousDesignatedBodyCode(OLD_DBC).updatedBy(UPDATED_BY)
        .eventDateTime(EVENT_DATETIME).build();

    rabbitMessageListener.receiveConnectionLog(connectionLogDto);

    verify(connectionService).recordConnectionLog(connectionLogDtoArgumentCaptor.capture());

    ConnectionLogDto result = connectionLogDtoArgumentCaptor.getValue();

    assertThat(result.getGmcId(), is(GMC_ID));
    assertThat(result.getNewDesignatedBodyCode(), is(NEW_DBC));
    assertThat(result.getPreviousDesignatedBodyCode(), is(OLD_DBC));
    assertThat(result.getUpdatedBy(), is(UPDATED_BY));
    assertThat(result.getEventDateTime(), is(EVENT_DATETIME));
  }

  @Test
  void shouldStartConnectionLogEsSync() {
    rabbitMessageListener.connectionLogEsSync(
        RabbitMessageListener.CONNECTION_LOG_SYNC_START_MESSAGE);
    verify(connectionService).sendConnectionLogsForSync(rabbitMessageListener.batchSize);
  }

  @Test
  void shouldNotStartConnectionLogEsSyncForInvalidMessage() {
    String invalidMessage = "invalidMessage";
    rabbitMessageListener.connectionLogEsSync(invalidMessage);
    verifyNoInteractions(connectionService);
  }
}
