package com.portal.conecta.comunicados.module.comunicado.infrastructure.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging.notification")
public record NotificationPublisherProperties(
        String exchange,
        String routingKey
) {}
