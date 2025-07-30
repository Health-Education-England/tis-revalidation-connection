package uk.nhs.hee.tis.revalidation.connection.exception;

public class ConnectionQueryException extends Exception {

  private static final String MESSAGE = "Exception during search for %s, query: %s";

  public ConnectionQueryException(String target, String attemptedQuery, Throwable throwable) {
    super(String.format(MESSAGE, target, attemptedQuery), throwable);
  }
}
