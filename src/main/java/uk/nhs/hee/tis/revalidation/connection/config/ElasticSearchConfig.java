package uk.nhs.hee.tis.revalidation.connection.config;

import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

public class ElasticSearchConfig {
  public static final int SCROLL_TIMEOUT_MS = 30000;
  public static final IndexCoordinates MASTER_DOCTOR_INDEX = IndexCoordinates.of("masterdoctorindex");
  public static final IndexCoordinates CONNECTED_INDEX = IndexCoordinates.of("connectedindex");
  public static final IndexCoordinates DISCONNECTED_INDEX = IndexCoordinates.of("disconnectedindex");
  public static final IndexCoordinates EXCEPTIONS_INDEX = IndexCoordinates.of("exceptionindex");

}
