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

package uk.nhs.hee.tis.revalidation.connection.entity;

public enum AddConnectionReasonCode {

  DOCTOR_HAS_CONNECTION_WITH_DBC("1", "The doctor has a connection with this designated body"),
  CONFLICT_OF_INTEREST("2", "Conflict of Interest");

  final String code;
  final String message;

  AddConnectionReasonCode(final String code, final String message) {
    this.code = code;
    this.message = message;
  }

  /**
   * Map add connection reason from code to message.
   *
   * @param code add connection reason code
   * @return add connection reason message
   */
  public static String fromCode(final String code) {
    for (final var responseCode : AddConnectionReasonCode.values()) {
      if (responseCode.getCode().equals(code)) {
        return responseCode.message;
      }
    }
    return null;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
