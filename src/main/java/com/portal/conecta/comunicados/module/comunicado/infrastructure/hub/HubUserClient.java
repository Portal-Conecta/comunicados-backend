package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubUserPort;
import com.portal.conecta.comunicados.shared.context.UserType;

@Component
public class HubUserClient implements HubUserPort {

    private final RestClient restClient;
    private final String hubBaseUrl;

    public HubUserClient(RestClient.Builder restClientBuilder, @Value("${hub.base-url}") String hubBaseUrl) {
        this.restClient = restClientBuilder.build();
        this.hubBaseUrl = hubBaseUrl;
    }

    @Override
    public Optional<UserType> findUserTypeById(UUID userId) {
        try {
            HubUserResponse response = restClient.get()
                    .uri(hubBaseUrl + "/api/users/{userId}", userId)
                    .retrieve()
                    .body(HubUserResponse.class);
            if (response == null || response.userType() == null) {
                return Optional.empty();
            }
            return Optional.of(UserType.valueOf(response.userType()));
        } catch (RestClientException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private record HubUserResponse(String userType) {
    }
}
