package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub;

import com.portal.conecta.comunicados.module.comunicado.domain.port.hub.HubCoursePort;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.dto.HubCourseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "hub.api", name = "mock-enabled", havingValue = "false")
public class HubCourseHttpAdapter implements HubCoursePort {

    private static final String MY_COURSES_PATH = "/me/courses";

    private final RestClient hubRestClient;

    public HubCourseHttpAdapter(@Qualifier("hubRestClient") RestClient hubRestClient) {
        this.hubRestClient = hubRestClient;
    }

    @Override
    public List<UUID> getCurrentUserCourseIds() {
        String authorization = currentAuthorizationHeader();
        if (authorization == null || authorization.isBlank()) {
            return List.of();
        }

        try {
            HubMyCoursesResponse response = hubRestClient.get()
                    .uri(MY_COURSES_PATH)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(HubMyCoursesResponse.class);

            if (response == null || response.courses() == null) {
                return List.of();
            }

            return response.courses()
                    .stream()
                    .map(HubMyCoursesResponse.HubCourse::id)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (RestClientException exception) {
            log.warn("Failed to resolve user courses from Hub. Falling back to empty course scope.", exception);
            return List.of();
        }
    }

    @Override
    public Optional<String> findCourseNameById(UUID courseId) {
        try {
            HubCourseResponse response = hubRestClient.get()
                    .uri("/courses/{courseId}", courseId)
                    .retrieve()
                    .body(HubCourseResponse.class);

            if (response == null || response.name() == null || response.name().isBlank()) {
                return Optional.empty();
            }

            return Optional.of(response.name().trim());
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        } catch (RestClientException exception) {
            log.warn("Failed to resolve course {} from Hub.", courseId, exception);
            return Optional.empty();
        }
    }

    private String currentAuthorizationHeader() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        }
        return null;
    }
}
