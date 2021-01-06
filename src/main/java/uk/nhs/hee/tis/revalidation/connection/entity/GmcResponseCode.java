package uk.nhs.hee.tis.revalidation.connection.entity;

public enum GmcResponseCode {

  SUCCESS("0", "Success"),
  MISSING_OR_INVALID_GMC_REF_NUMBER("70", "Missing / Invalid Doctor GMC reference number"),
  MISSING_OR_INVALID_DESIGNATED_BODY("80", "Missing / Invalid Designated Body code"),
  YOUR_ACCOUNT_DOES_NOT_HAVE_ACCESS_TO_DB("81", "Your account does not have access to this DB"),
  DOCTOR_NOT_SUBJECT_TO_REVALIDATION("90", "Doctor not subject to revalidation"),
  INTERNAL_ERROR("98", "Internal error"),
  INVALID_CREDENTIALS("99", "Invalid Credentials (user name / password / IP address)"),
  DOCTOR_ALREADY_ASSOCIATED("100", "Doctor already associated with your Designated Body"),
  MISSING_INTERNAL_USER("110", "Missing Internal User"),
  MISSING_OR_INVALID_REASON_CODE("120", "Missing / Invalid Change Code (Reason)"),
  DOCTOR_NOT_ASSOCIATED_WITH_DESIGNATED_BODY("140",
      "Doctor not associated with your Designated Body"),
  DOCTOR_DB_HISTORY_LOCKED("160",
      "Doctor’s DB History is locked, please contact the GMC Revalidation team");

  final String code;
  final String message;

  GmcResponseCode(final String code, final String message) {
    this.code = code;
    this.message = message;
  }

  /**
   * Map gmc response code
   *
   * @param code string of gmc response code
   * @return gmc response code
   */
  public static GmcResponseCode fromCode(final String code) {
    for (final var responseCode : GmcResponseCode.values()) {
      if (responseCode.getCode().equals(code)) {
        return responseCode;
      }
    }
    return null;
  }

  /**
   * Map gmc response from code to message
   *
   * @param code gmc response code
   * @return gmc response message
   */
  public static String fromCodeToMessage(final String code) {
    for (final var responseCode : GmcResponseCode.values()) {
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
