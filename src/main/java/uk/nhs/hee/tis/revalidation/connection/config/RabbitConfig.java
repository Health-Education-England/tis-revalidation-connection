package uk.nhs.hee.tis.revalidation.connection.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {


  @Value("${app.rabbit.exchange}")
  private String exchange;

  @Value("${app.rabbit.connection.queue}")
  private String queueName;

  @Value("${app.rabbit.connection.routingKey}")
  private String routingKey;

  @Value("${app.rabbit.es-exchange}")
  private String esExchange;

  @Value("${app.rabbit.es-queue}")
  private String esQueueName;

  @Value("${app.rabbit.es-connections-queue}")
  private String esConnectionsQueueName;

  @Value("${app.rabbit.es-routingKey}")
  private String esRoutingKey;

  @Value("${app.rabbit.es-connections-routingKey}")
  private String esConnectionsRoutingKey;

  @Bean
  public Queue queue() {
    return new Queue(queueName, false);
  }

  @Bean
  public Queue esQueue() {
    return new Queue(esQueueName, false);
  }

  @Bean
  public Queue esConnectionsQueue() {
    return new Queue(esConnectionsQueueName, false);
  }

  @Bean
  public DirectExchange exchange() {
    return new DirectExchange(exchange);
  }

  @Bean
  public DirectExchange esExchange() {
    return new DirectExchange(esExchange);
  }


  @Bean
  public Binding binding(final Queue queue, final DirectExchange exchange) {
    return BindingBuilder.bind(queue).to(exchange).with(routingKey);

  }

  @Bean
  public Binding esBinding(final Queue esQueue, final DirectExchange esExchange) {
    return BindingBuilder.bind(esQueue).to(esExchange).with(esRoutingKey);
  }

  @Bean
  public Binding esConnectionsBinding(final Queue esConnectionsQueue, final DirectExchange esExchange) {
    return BindingBuilder.bind(esConnectionsQueue).to(esExchange).with(esConnectionsRoutingKey);
  }

  @Bean
  public MessageConverter jsonMessageConverter() {
    final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    return new Jackson2JsonMessageConverter(mapper);
  }

  @Bean
  public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
    final var rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(jsonMessageConverter());
    rabbitTemplate.containerAckMode(AcknowledgeMode.AUTO);
    return rabbitTemplate;
  }
}
