package uk.nhs.hee.tis.revalidation.connection.service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
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

  public StringConverter encodeUrl() {
    if (this.str != null) {
      try {
        this.str = URLEncoder.encode(this.str, "UTF-8");
      } catch (UnsupportedEncodingException var2) {
        LOGGER.warn("Unable to URL encode string.", var2);
      }
    }

    return this;
  }

  public StringConverter decodeUrl() {
    if (this.str != null) {
      try {
        this.str = URLDecoder.decode(this.str, "UTF-8");
        this.filterInvisible();
      } catch (IllegalArgumentException | UnsupportedEncodingException var2) {
        LOGGER.warn("Unable to URL decode string.", var2);
      }
    }

    return this;
  }

  public StringConverter escapeForElasticSearch() {
    if (this.str != null) {
      this.str = QueryParserBase.escape(this.str);
    }

    return this;
  }

  public StringConverter escapeForJson() {
    if (this.str != null) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < this.str.length(); ++i) {
        char c = this.str.charAt(i);
        if (c == '\\' || c == '"') {
          sb.append('\\');
        }

        sb.append(c);
      }

      this.str = sb.toString();
    }

    return this;
  }

  public StringConverter escapeForSql() {
    if (this.str != null) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < this.str.length(); ++i) {
        char c = this.str.charAt(i);
        if (c == '\\' || c == '"' || c == '\'' || c == '%' || c == '_') {
          sb.append('\\');
        }

        sb.append(c);
      }

      this.str = sb.toString();
    }

    return this;
  }

  public StringConverter fromJson() {
    if (this.str != null) {
      this.str = this.str.replaceAll("^\"(.*)\"$", "$1");
    }

    return this;
  }

  public String toString() {
    return this.str;
  }
}
