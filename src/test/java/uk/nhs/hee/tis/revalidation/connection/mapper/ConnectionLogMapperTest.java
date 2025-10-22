package uk.nhs.hee.tis.revalidation.connection.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.javafaker.Faker;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionLogDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionLog;

public class ConnectionLogMapperTest {

  private static final Faker faker = Faker.instance();

  private static final String GMC_ID = UUID.randomUUID().toString();
  private static final String NEW_DBC = faker.lorem().characters(8);
  private static final String OLD_DBC = faker.lorem().characters(8);
  private static final String UPDATED_BY = faker.lorem().characters(8);
  private static final LocalDateTime EVENT_DATETIME = LocalDateTime.now();

  ConnectionLogMapper mapper = new ConnectionLogMapperImpl();

  @Test
  void shouldMapConnectionLogToDto() {
    ConnectionLog connectionLog = ConnectionLog.builder().gmcId(GMC_ID)
        .newDesignatedBodyCode(NEW_DBC)
        .previousDesignatedBodyCode(OLD_DBC).updatedBy(UPDATED_BY).requestTime(EVENT_DATETIME)
        .build();

    ConnectionLogDto result = mapper.toDto(connectionLog);

    assertThat(result.getGmcId(), is(GMC_ID));
    assertThat(result.getNewDesignatedBodyCode(), is(NEW_DBC));
    assertThat(result.getPreviousDesignatedBodyCode(), is(OLD_DBC));
    assertThat(result.getUpdatedBy(), is(UPDATED_BY));
    assertThat(result.getEventDateTime(), is(EVENT_DATETIME));
  }

  @Test
  void shouldMapConnectionLogDtoToConnectionLog() {
    ConnectionLogDto connectionLogDto = ConnectionLogDto.builder().gmcId(GMC_ID)
        .newDesignatedBodyCode(NEW_DBC)
        .previousDesignatedBodyCode(OLD_DBC).updatedBy(UPDATED_BY).eventDateTime(EVENT_DATETIME)
        .build();

    ConnectionLog result = mapper.fromDto(connectionLogDto);

    assertThat(result.getGmcId(), is(GMC_ID));
    assertThat(result.getNewDesignatedBodyCode(), is(NEW_DBC));
    assertThat(result.getPreviousDesignatedBodyCode(), is(OLD_DBC));
    assertThat(result.getUpdatedBy(), is(UPDATED_BY));
    assertThat(result.getRequestTime(), is(EVENT_DATETIME));
  }

}
