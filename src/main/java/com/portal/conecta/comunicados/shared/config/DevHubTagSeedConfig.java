package com.portal.conecta.comunicados.shared.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
@EnableConfigurationProperties(DevHubTagSeedProperties.class)
public class DevHubTagSeedConfig {
}
