package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.domain.port.hub.HubCoursePort;

@Component
@ConditionalOnProperty(prefix = "hub.api", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockHubCourseAdapter implements HubCoursePort {

    @Override
    public List<UUID> getCurrentUserCourseIds() {
        return List.of();
    }
}
