package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.adapter;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubRoomPort;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.properties.HubMockProperties;

@Component
@ConditionalOnProperty(prefix = "hub.api", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockHubRoomAdapter implements HubRoomPort {

    private final Set<UUID> roomIds;
    private final Set<String> userRoomLinks;

    public MockHubRoomAdapter(HubMockProperties properties) {
        this.roomIds = properties.roomIds().stream()
                .map(UUID::fromString)
                .collect(Collectors.toUnmodifiableSet());
        this.userRoomLinks = Set.copyOf(properties.userRoomLinks());
    }

    @Override
    public boolean existsById(UUID roomId) {
        return roomIds.isEmpty() || roomIds.contains(roomId);
    }

    @Override
    public boolean isUserLinkedToRoom(UUID userId, UUID roomId) {
        if (userRoomLinks.isEmpty()) {
            return existsById(roomId);
        }

        return userRoomLinks.contains(linkKey(userId, roomId));
    }

    private static String linkKey(UUID userId, UUID roomId) {
        return userId + ":" + roomId;
    }
}
