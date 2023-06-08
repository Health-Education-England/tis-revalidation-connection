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
class ElasticsearchQueryHelperTest {

  @Test
  void shouldFormatDesignatedBodyCodesForElasticsearchQuery() {
    final String dbcformatted = "aiidhj aiidmq";
    List<String> dbcs = List.of("1-AIIDHJ", "AIIDMQ");

    final var result = ElasticsearchQueryHelper.formatDesignatedBodyCodesForElasticsearchQuery(
        dbcs);

    assertThat(result, Matchers.is(dbcformatted));
  }
}
