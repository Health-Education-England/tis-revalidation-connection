package uk.nhs.hee.tis.revalidation.connection.exception;

public class ConnectionQueryException extends Exception {

  private final static String message = "Exception during search for {}, query: {}";
  public ConnectionQueryException(String target, String attemptedQuery, Throwable throwable) {
    super(String.format(message, target, attemptedQuery), throwable);
  }
}
