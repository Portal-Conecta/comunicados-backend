package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.properties.HubApiProperties;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.properties.HubMockProperties;

@Configuration
@EnableConfigurationProperties({HubApiProperties.class, HubMockProperties.class})
public class HubInfrastructureConfig {

    @Bean
    public RestClient.Builder hubRestClientBuilder(HubAuthForwardingInterceptor authForwardingInterceptor) {
        return RestClient.builder().requestInterceptor(authForwardingInterceptor);
    }
}
