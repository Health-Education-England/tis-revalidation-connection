package uk.nhs.hee.tis.revalidation.connection.entity;


/**
 * The Status enumeration.
 */
public enum Status {
  CURRENT("current"),
  INACTIVE("inactive"),
  DELETE("delete");

  private final String text;

  Status(final String s) {
    text = s;
  }

  public static Status fromString(String text) {
    for (Status status : Status.values()) {
      if (status.text.equalsIgnoreCase(text)) {
        return status;
      }
    }
    return null;
  }

  public String toString() {
    return text;
  }
}
