package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.adapter;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubRoomPort;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.exception.HubIntegrationException;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.properties.HubApiProperties;

@Component
@ConditionalOnProperty(prefix = "hub.api", name = "mock-enabled", havingValue = "false")
public class HttpHubRoomAdapter implements HubRoomPort {

    private final RestClient restClient;

    public HttpHubRoomAdapter(
            HubApiProperties properties,
            @Qualifier("hubRestClientBuilder") RestClient.Builder restClientBuilder
    ) {
        this.restClient = restClientBuilder.baseUrl(properties.url()).build();
    }

    @Override
    public boolean existsById(UUID roomId) {
        try {
            restClient.get()
                    .uri("/rooms/{roomId}", roomId)
                    .retrieve()
                    .toBodilessEntity();

            return true;
        } catch (HttpClientErrorException.NotFound exception) {
            return false;
        } catch (RestClientException exception) {
            throw new HubIntegrationException("Serviço de salas do Hub indisponível.", exception);
        }
    }

    @Override
    public boolean isUserLinkedToRoom(UUID userId, UUID roomId) {
        try {
            restClient.get()
                    .uri("/users/{userId}/rooms/{roomId}", userId, roomId)
                    .retrieve()
                    .toBodilessEntity();

            return true;
        } catch (HttpClientErrorException.NotFound exception) {
            return false;
        } catch (RestClientException exception) {
            throw new HubIntegrationException("Serviço de vínculo usuário-sala do Hub indisponível.", exception);
        }
    }
}
