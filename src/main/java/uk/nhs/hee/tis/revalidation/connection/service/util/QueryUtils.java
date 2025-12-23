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

import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;

public final class QueryUtils {

  private QueryUtils() {
  }

  /**
   * A common method that adds a range filter to the root query.
   *
   * @param rootQuery         root query
   * @param field             field to be filtered
   * @param fromDateInclusive from date inclusive
   * @param toDateInclusive   to date inclusive
   */
  public static void addDateRangeFilter(BoolQueryBuilder rootQuery,
      String field,
      String fromDateInclusive,
      String toDateInclusive) {
    if (fromDateInclusive == null && toDateInclusive == null) {
      return;
    }

    RangeQueryBuilder dateRange = rangeQuery(field);
    if (fromDateInclusive != null) {
      dateRange.gte(fromDateInclusive);
    }
    if (toDateInclusive != null) {
      dateRange.lte(toDateInclusive);
    }
    rootQuery.filter(dateRange);
  }
}
