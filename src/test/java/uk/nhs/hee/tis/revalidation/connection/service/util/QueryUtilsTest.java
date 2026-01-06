/*
 * The MIT License (MIT)
 *
 * Copyright 2025 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.revalidation.connection.service.util;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.time.LocalTime;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.revalidation.connection.service.util.EsQueryUtils.DATE_RANGE_QUERY_TYPE;

class QueryUtilsTest {

  private static final String FIELD = "membershipEndDate";

  @Test
  void shouldNotModifyRootQueryWhenBothFromAndToFieldsAreNull() {
    final BoolQueryBuilder rootQuery = boolQuery();
    final String before = rootQuery.toString();

    EsQueryUtils.addDateRangeFilter(rootQuery, FIELD, null, null);

    final String after = rootQuery.toString();
    assertThat(after, is(before));
  }

  @Test
  void shouldAddGteWhenFromDateHasNonNullAndToDateHasNullValue() {
    final BoolQueryBuilder rootQuery = boolQuery();
    final String from = "2024-01-01";

    EsQueryUtils.addDateRangeFilter(rootQuery, FIELD, from, null);

    final String after = rootQuery.toString();
    assertThat(after, containsString(FIELD));
    assertThat(after, containsString("\"from\" : \"2024-01-01\""));
    assertThat(after, containsString("\"to\" : null"));
  }

  @Test
  void shouldAddLteWhenFromDateHasNullAndToDateHasNonNullValue() {
    final BoolQueryBuilder rootQuery = boolQuery();
    final String to = "2024-12-31";

    EsQueryUtils.addDateRangeFilter(rootQuery, FIELD, null, to);

    final String after = rootQuery.toString();
    assertThat(after, containsString(FIELD));
    assertThat(after, containsString("\"from\" : null"));
    assertThat(after, containsString("\"to\" : \"2024-12-31\""));
  }

  @Test
  void shouldAddBothGteAndLteWhenBothFromDateAndToDateHaveNonNullValue() {
    final BoolQueryBuilder rootQuery = boolQuery();
    final String from = "2024-01-01";
    final String to = "2024-12-31";

    EsQueryUtils.addDateRangeFilter(rootQuery, FIELD, from, to);

    final String after = rootQuery.toString();
    assertThat(after, containsString(FIELD));
    assertThat(after, containsString("\"from\" : \"2024-01-01\""));
    assertThat(after, containsString("\"to\" : \"2024-12-31\""));
  }

  @Test
  void testShouldPassWhenAddDateRangeFilterReceivesADifferentFieldName() {
    final BoolQueryBuilder rootQuery = boolQuery();
    final String customField = "submissionDate";
    final String from = "2021-01-01";
    final String to = "2021-12-31";

    EsQueryUtils.addDateRangeFilter(rootQuery, customField, from, to);

    String after = rootQuery.toString();
    assertThat(after, containsString("\"" + customField + "\" : "));
  }

  @Test
  void shouldFormatDateFromQuery() {
    LocalDate localDate = LocalDate.of(2026,1,1);

    String result = EsQueryUtils.getDateTimeQueryFromRange(localDate, DATE_RANGE_QUERY_TYPE.FROM);

    assertThat(result, is("2026-01-01T00:00:00.000"));
  }

  @Test
  void shouldFormatDateToQuery() {
    LocalDate localDate = LocalDate.of(2026,1,1);

    String result = EsQueryUtils.getDateTimeQueryFromRange(localDate, DATE_RANGE_QUERY_TYPE.TO);

    assertThat(result, is("2026-01-01T23:59:59.000"));
  }
}
