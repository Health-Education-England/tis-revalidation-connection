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

  @Value("${app.rabbit.reval.exchange}")
  private String esExchange;

  @Value("${app.rabbit.reval.queue.connection.update}")
  private String esTisQueueName;

  @Value("${app.rabbit.reval.routingKey.connection.update}")
  private String esTisRoutingKey;

  @Value("${app.rabbit.reval.queue.connection.getmaster}")
  private String esGetMasterQueueName;

  @Value("${app.rabbit.reval.routingKey.connection.getmaster}")
  private String esGetMasterRoutingKey;

  @Value("${app.rabbit.reval.queue.connection.gmcupdate}")
  private String esGmcQueueName;

  @Value("${app.rabbit.reval.routingKey.gmc.update}")
  private String esGmcRoutingKey;

  @Bean
  public Queue queue() {
    return new Queue(queueName, false);
  }

  @Bean
  public Queue esTisQueue() {
    return new Queue(esTisQueueName, false);
  }

  @Bean
  public Queue esGetMasterQueue() {
    return new Queue(esGetMasterQueueName, false);
  }

  @Bean
  public Queue esGmcQueue() {
    return new Queue(esGmcQueueName, false);
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
  public Binding esTisBinding(final Queue esTisQueue, final DirectExchange esExchange) {
    return BindingBuilder.bind(esTisQueue).to(esExchange).with(esTisRoutingKey);
  }

  @Bean
  public Binding esGetMasterBinding(final Queue esGetMasterQueue, final DirectExchange esExchange) {
    return BindingBuilder.bind(esGetMasterQueue).to(esExchange).with(esGetMasterRoutingKey);
  }

  @Bean
  public Binding esGmcBinding(final Queue esGmcQueue, final DirectExchange exchange) {
    return BindingBuilder.bind(esGmcQueue).to(exchange).with(esGmcRoutingKey);
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
