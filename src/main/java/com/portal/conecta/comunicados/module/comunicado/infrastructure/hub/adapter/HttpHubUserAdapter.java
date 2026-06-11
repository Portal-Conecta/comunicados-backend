package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.portal.conecta.comunicados.module.comunicado.domain.model.hub.HubUser;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubUserPort;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.dto.HubUserResponse;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.exception.HubIntegrationException;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.properties.HubApiProperties;
import com.portal.conecta.comunicados.shared.context.UserType;

@Component
@ConditionalOnProperty(prefix = "hub.api", name = "mock-enabled", havingValue = "false")
public class HttpHubUserAdapter implements HubUserPort {

    private final RestClient restClient;

    public HttpHubUserAdapter(
            HubApiProperties properties,
            @Qualifier("hubRestClientBuilder") RestClient.Builder restClientBuilder
    ) {
        this.restClient = restClientBuilder.baseUrl(properties.url()).build();
    }

    @Override
    public boolean existsById(UUID userId) {
        try {
            restClient.get()
                    .uri("/users/{userId}", userId)
                    .retrieve()
                    .toBodilessEntity();

            return true;
        } catch (HttpClientErrorException.NotFound exception) {
            return false;
        } catch (RestClientException exception) {
            throw new HubIntegrationException("Serviço de usuários do Hub indisponível.", exception);
        }
    }

    @Override
    public Optional<HubUser> findById(UUID userId) {
        try {
            HubUserResponse response = restClient.get()
                    .uri("/users/{userId}", userId)
                    .retrieve()
                    .body(HubUserResponse.class);

            if (response == null) {
                return Optional.empty();
            }

            return Optional.of(toHubUser(response));
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        } catch (RestClientException exception) {
            throw new HubIntegrationException("Serviço de usuários do Hub indisponível.", exception);
        }
    }

    @Override
    public Optional<UserType> findUserTypeById(UUID userId) {
        return findById(userId).flatMap(user -> Optional.ofNullable(user.userType()));
    }

    private HubUser toHubUser(HubUserResponse response) {
        UserType userType = null;
        if (response.userType() != null && !response.userType().isBlank()) {
            try {
                userType = UserType.valueOf(response.userType());
            } catch (IllegalArgumentException ignored) {
                userType = null;
            }
        }
        return new HubUser(response.id(), response.name(), userType);
    }
}
