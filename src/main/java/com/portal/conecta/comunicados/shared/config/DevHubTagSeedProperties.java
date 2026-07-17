package com.portal.conecta.comunicados.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.dev-seed.hub-tags")
public record DevHubTagSeedProperties(
        boolean enabled,
        String email,
        String password,
        int maxAttempts,
        long retryDelayMs
) {
    public DevHubTagSeedProperties {
        if (email == null || email.isBlank()) {
            email = "admin@portal.test";
        }
        if (password == null || password.isBlank()) {
            password = "123456";
        }
        if (maxAttempts <= 0) {
            maxAttempts = 12;
        }
        if (retryDelayMs <= 0) {
            retryDelayMs = 3000L;
        }
    }
}
