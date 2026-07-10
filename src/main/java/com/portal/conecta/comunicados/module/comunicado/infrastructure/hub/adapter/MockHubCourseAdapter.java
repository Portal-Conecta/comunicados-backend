package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.domain.port.hub.HubCoursePort;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.properties.HubMockProperties;

import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnProperty(prefix = "hub.api", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class MockHubCourseAdapter implements HubCoursePort {

    private final HubMockProperties properties;

    @Override
    public List<UUID> getCurrentUserCourseIds() {
        return List.of();
    }

    @Override
    public Optional<String> findCourseNameById(UUID courseId) {
        String name = properties.courseNamesById().get(courseId.toString());
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(name);
    }
}
