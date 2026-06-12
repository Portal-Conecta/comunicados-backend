package com.portal.conecta.comunicados.module.tag.infrastructure.messaging.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(
        boolean enabled,
        CoreMessagingProperties core
) {

    public record CoreMessagingProperties(
            String exchange,
            String queue,
            String dlq,
            List<String> routingKeys
    ) {
        public CoreMessagingProperties {
            if (routingKeys == null) {
                routingKeys = List.of();
            }
        }
    }
}
