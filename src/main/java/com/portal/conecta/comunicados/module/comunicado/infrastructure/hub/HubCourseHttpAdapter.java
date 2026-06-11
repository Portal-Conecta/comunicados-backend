package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub;

import com.portal.conecta.comunicados.module.comunicado.domain.port.hub.HubCoursePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@Slf4j
public class HubCourseHttpAdapter implements HubCoursePort {

    private static final String MY_COURSES_PATH = "/me/courses";

    private final RestClient hubRestClient;

    public HubCourseHttpAdapter(RestClient hubRestClient) {
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

    private String currentAuthorizationHeader() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        }
        return null;
    }
}
