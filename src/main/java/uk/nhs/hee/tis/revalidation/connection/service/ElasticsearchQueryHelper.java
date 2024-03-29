/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.revalidation.connection.service;

import static java.util.stream.Collectors.joining;

import java.util.List;

public final class ElasticsearchQueryHelper {

  private static List<String> sortFields = List.of("designatedBody","tcsDesignatedBody");

  private ElasticsearchQueryHelper() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Format designated body codes for Elasticsearch by stripping hyphens and setting lower case.
   *
   * @param designatedBodyCodes list of designated body codes to format
   *
   */
  public static String formatDesignatedBodyCodesForElasticsearchQuery(
      List<String> designatedBodyCodes) {
    return designatedBodyCodes.stream().map(
        dbc -> dbc.toLowerCase().replace("1-", "")
    ).collect(joining(" "));
  }

  /**
   * Format sort columns to add .keyword suffix where required.
   *
   * @param sortField name of the field to sort by
   *
   */
  public static String formatSortFieldForElasticsearchQuery(String sortField) {
    if (sortFields.contains(sortField)) {
      return sortField.concat(".keyword");
    }
    return sortField;
  }
}
