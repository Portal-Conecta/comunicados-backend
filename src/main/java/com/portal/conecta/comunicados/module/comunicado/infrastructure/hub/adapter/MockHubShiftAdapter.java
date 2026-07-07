package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.adapter;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.domain.port.hub.HubShiftPort;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubClassPort;

import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnProperty(prefix = "hub.api", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class MockHubShiftAdapter implements HubShiftPort {

    private final HubClassPort hubClassPort;

    @Override
    public List<String> getShiftCodesForClasses(List<UUID> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return List.of();
        }

        return classIds.stream()
                .map(hubClassPort::findClassById)
                .flatMap(java.util.Optional::stream)
                .map(info -> info.shiftCode())
                .filter(Objects::nonNull)
                .filter(code -> !code.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }
}
