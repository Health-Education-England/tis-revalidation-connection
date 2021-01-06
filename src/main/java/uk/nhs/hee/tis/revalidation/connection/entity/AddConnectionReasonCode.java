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
