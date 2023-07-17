package uk.nhs.hee.tis.revalidation.connection.service;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ElasticsearchQueryHelperTest {

  private final String keywordField = "keywordField";
  private final String formattedKeywordField = "keywordField.keyword";

  @Test
  void shouldFormatDesignatedBodyCodesForElasticsearchQuery() {
    final String dbcformatted = "aiidhj aiidmq";
    List<String> dbcs = List.of("1-AIIDHJ", "AIIDMQ");

    final var result = ElasticsearchQueryHelper.formatDesignatedBodyCodesForElasticsearchQuery(
        dbcs);

    assertThat(result, Matchers.is(dbcformatted));
  }

  @Test
  void shouldAddKeywordSuffixToKeywordSortFields() {
    ReflectionTestUtils
        .setField(ElasticsearchQueryHelper.class, "sortFields", List.of(keywordField));
    final var result = ElasticsearchQueryHelper.formatSortFieldForElasticsearchQuery(keywordField);

    assertThat(result, Matchers.is(formattedKeywordField));
  }
}
