package uk.nhs.hee.tis.revalidation.connection.entity;

public enum RemoveConnectionReasonCode {

  CONFLICT_OF_INTEREST("1", "Conflict of Interest"),
  DOCTOR_HAS_RETIRED("2", "Doctor has retired"),
  DOCTOR_HAS_NO_CONNECTION_WITH_DBC("3",
      "The doctor does not have a connection with this designated body");

  final String code;
  final String message;

  RemoveConnectionReasonCode(final String code, final String message) {
    this.code = code;
    this.message = message;
  }

  // get message reason from code
  public static String fromCode(final String code) {
    for (final var responseCode : RemoveConnectionReasonCode.values()) {
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
