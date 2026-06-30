package com.portal.conecta.comunicados.module.tag.infrastructure.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitMqConfig {

    @Bean
    TopicExchange classEventsExchange(MessagingProperties properties) {
        return new TopicExchange(properties.core().classExchange().name(), true, false);
    }

    @Bean
    TopicExchange courseEventsExchange(MessagingProperties properties) {
        return new TopicExchange(properties.core().courseExchange().name(), true, false);
    }

    @Bean
    Queue coreEntitiesDlq(MessagingProperties properties) {
        return QueueBuilder.durable(properties.core().dlq()).build();
    }

    @Bean
    Queue coreEntitiesQueue(MessagingProperties properties) {
        return QueueBuilder.durable(properties.core().queue())
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", properties.core().dlq())
                .build();
    }

    @Bean
    Binding[] classEntityBindings(
            Queue coreEntitiesQueue,
            TopicExchange classEventsExchange,
            MessagingProperties properties
    ) {
        return properties.core().classExchange().routingKeys().stream()
                .map(routingKey -> BindingBuilder
                        .bind(coreEntitiesQueue)
                        .to(classEventsExchange)
                        .with(routingKey))
                .toArray(Binding[]::new);
    }

    @Bean
    Binding[] courseEntityBindings(
            Queue coreEntitiesQueue,
            TopicExchange courseEventsExchange,
            MessagingProperties properties
    ) {
        return properties.core().courseExchange().routingKeys().stream()
                .map(routingKey -> BindingBuilder
                        .bind(coreEntitiesQueue)
                        .to(courseEventsExchange)
                        .with(routingKey))
                .toArray(Binding[]::new);
    }

    @Bean
    MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
