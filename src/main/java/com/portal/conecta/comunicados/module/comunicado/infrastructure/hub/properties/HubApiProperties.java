package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hub.api")
public record HubApiProperties(
        String url,
        boolean mockEnabled
) {

    public HubApiProperties {
        if (!mockEnabled && (url == null || url.isBlank())) {
            throw new IllegalStateException("hub.api.url deve ser configurado quando hub.api.mock-enabled=false.");
        }
    }
}
