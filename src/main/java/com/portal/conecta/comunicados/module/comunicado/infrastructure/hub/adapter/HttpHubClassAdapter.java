package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.adapter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.portal.conecta.comunicados.module.comunicado.domain.model.hub.HubClassInfo;
import com.portal.conecta.comunicados.module.comunicado.domain.model.hub.HubStudent;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubClassPort;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.dto.HubClassMemberResponse;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.dto.HubClassResponse;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.exception.HubIntegrationException;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.properties.HubApiProperties;

@Component
@ConditionalOnProperty(prefix = "hub.api", name = "mock-enabled", havingValue = "false")
public class HttpHubClassAdapter implements HubClassPort {

    private final RestClient restClient;

    public HttpHubClassAdapter(
            HubApiProperties properties,
            @Qualifier("hubRestClientBuilder") RestClient.Builder restClientBuilder
    ) {
        this.restClient = restClientBuilder.baseUrl(properties.url()).build();
    }

    @Override
    public Optional<HubClassInfo> findClassById(UUID classId) {
        try {
            HubClassResponse response = restClient.get()
                    .uri("/classes/{classId}", classId)
                    .retrieve()
                    .body(HubClassResponse.class);

            if (response == null || response.id() == null) {
                return Optional.empty();
            }

            return Optional.of(new HubClassInfo(response.id(), response.name(), response.shift()));
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        } catch (RestClientException exception) {
            throw new HubIntegrationException("Serviço de turmas do Hub indisponível.", exception);
        }
    }

    @Override
    public boolean existsById(UUID classId) {
        try {
            restClient.get()
                    .uri("/classes/{classId}", classId)
                    .retrieve()
                    .toBodilessEntity();

            return true;
        } catch (HttpClientErrorException.NotFound exception) {
            return false;
        } catch (RestClientException exception) {
            throw new HubIntegrationException("Serviço de turmas do Hub indisponível.", exception);
        }
    }

    @Override
    public List<HubStudent> findMyClassStudents() {
        try {
            HubClassMemberResponse[] members = restClient.get()
                    .uri("/me/classes/students")
                    .retrieve()
                    .body(HubClassMemberResponse[].class);

            if (members == null || members.length == 0) {
                return List.of();
            }

            return Arrays.stream(members)
                    .filter(member -> member != null && member.id() != null)
                    .map(member -> new HubStudent(member.id(), member.name()))
                    .toList();
        } catch (HttpClientErrorException.NotFound exception) {
            return List.of();
        } catch (RestClientException exception) {
            throw new HubIntegrationException("Serviço de aprendizes do Hub indisponível.", exception);
        }
    }

    @Override
    public UUID getClassIdForUser(UUID userId) {
        try {
            return restClient.get()
                    .uri("/users/{userId}/class", userId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<UUID>() {});
        } catch (HttpClientErrorException.NotFound exception) {
            return null;
        } catch (RestClientException exception) {
            throw new HubIntegrationException("Serviço de turmas do Hub indisponível.", exception);
        }
    }
}
