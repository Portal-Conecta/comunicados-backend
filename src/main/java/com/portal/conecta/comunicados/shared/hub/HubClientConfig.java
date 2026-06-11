package com.portal.conecta.comunicados.shared.hub;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class HubClientConfig {

    @Bean
    public RestClient hubRestClient(@Value("${hub.base-url}") String hubBaseUrl) {
        return RestClient.builder()
                .baseUrl(hubBaseUrl)
                .build();
    }
}
