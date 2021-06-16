package uk.nhs.hee.tis.revalidation.connection.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {
  public static final List<String> ES_INDICES = List
      .of("connectedindex", "disconnectedindex", "exceptionindex");
}
