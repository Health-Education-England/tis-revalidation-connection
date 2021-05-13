/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
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

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringConverter {

  private static final Logger LOGGER = LoggerFactory.getLogger(StringConverter.class.getName());
  private String str;

  private StringConverter(String stringToConvert) {
    this.str = stringToConvert;
    this.filterInvisible();
  }

  public static StringConverter getConverter(String stringToConvert) {
    return new StringConverter(stringToConvert);
  }

  private void filterInvisible() {
    if (this.str != null) {
      this.str = this.str.replaceAll("\\p{C}", "").trim();
    }

  }

  /**
   * encode URL.
   */
  public StringConverter encodeUrl() {
    if (this.str != null) {
      this.str = URLEncoder.encode(this.str, StandardCharsets.UTF_8);
    }

    return this;
  }

  /**
   * decode URL.
   */
  public StringConverter decodeUrl() {
    if (this.str != null) {
      try {
        this.str = URLDecoder.decode(this.str, StandardCharsets.UTF_8);
        this.filterInvisible();
      } catch (IllegalArgumentException var2) {
        LOGGER.warn("Unable to URL decode string.", var2);
      }
    }

    return this;
  }

  /**
   * escape for Elastic Search.
   */
  public StringConverter escapeForElasticSearch() {
    if (this.str != null) {
      this.str = QueryParserBase.escape(this.str);
    }

    return this;
  }

  /**
   * escape for Json.
   */
  public StringConverter escapeForJson() {
    if (this.str != null) {
      var sb = new StringBuilder();

      for (var i = 0; i < this.str.length(); ++i) {
        var c = this.str.charAt(i);
        if (c == '\\' || c == '"') {
          sb.append('\\');
        }

        sb.append(c);
      }

      this.str = sb.toString();
    }

    return this;
  }

  /**
   * escape for SQL.
   */
  public StringConverter escapeForSql() {
    if (this.str != null) {
      var sb = new StringBuilder();

      for (var i = 0; i < this.str.length(); ++i) {
        var c = this.str.charAt(i);
        if (c == '\\' || c == '"' || c == '\'' || c == '%' || c == '_') {
          sb.append('\\');
        }

        sb.append(c);
      }

      this.str = sb.toString();
    }

    return this;
  }

  /**
   * from Json.
   */
  public StringConverter fromJson() {
    if (this.str != null) {
      this.str = this.str.replaceAll("^\"(.*)\"$", "$1");
    }

    return this;
  }

  /**
   * to string.
   */
  public String toString() {
    return this.str;
  }
}
