package com.portal.conecta.comunicados.module.comunicado.infrastructure.messaging.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(NotificationPublisherProperties.class)
public class NotificationPublisherRabbitMqConfig {

    @Bean
    TopicExchange notificationsExchange(NotificationPublisherProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }
}
