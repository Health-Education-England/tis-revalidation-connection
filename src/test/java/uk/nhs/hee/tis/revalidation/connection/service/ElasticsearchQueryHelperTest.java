package uk.nhs.hee.tis.revalidation.connection.service;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionSummaryDto;
import uk.nhs.hee.tis.revalidation.connection.exception.ConnectionQueryException;

@ExtendWith(MockitoExtension.class)
public class ElasticsearchQueryHelperTest {

  @Test
  void shouldFormatDesignatedBodyCodesForElasticsearchQuery() {
    final String dbc1 = "1-AIIDHJ";
    final String dbc2 = "AIIDMQ";
    final String dbcformatted = "aiidhj aiidmq";
    List<String> dbcs = List.of(dbc1, dbc2);

    final var result = ElasticsearchQueryHelper.formatDesignatedBodyCodesForElasticsearchQuery(
        dbcs);

    assertThat(result, Matchers.is(dbcformatted));
  }
}
