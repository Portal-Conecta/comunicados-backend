package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.adapter;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.domain.model.hub.HubUser;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubUserPort;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.properties.HubMockProperties;
import com.portal.conecta.comunicados.shared.context.UserType;

@Component
@ConditionalOnProperty(prefix = "hub.api", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockHubUserAdapter implements HubUserPort {

    private final Set<UUID> userIds;
    private final Map<UUID, String> userNames;
    private final Map<UUID, UserType> userTypes;

    public MockHubUserAdapter(HubMockProperties properties) {
        this.userIds = properties.userIds().stream()
                .map(UUID::fromString)
                .collect(Collectors.toUnmodifiableSet());

        this.userNames = properties.studentsByClass().values().stream()
                .flatMap(students -> students.stream())
                .collect(Collectors.toMap(
                        student -> UUID.fromString(student.id()),
                        HubMockProperties.MockStudent::name,
                        (left, right) -> left
                ));

        this.userTypes = properties.userTypesById().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        entry -> UUID.fromString(entry.getKey()),
                        entry -> UserType.valueOf(entry.getValue())
                ));
    }

    @Override
    public boolean existsById(UUID userId) {
        return userIds.isEmpty() || userIds.contains(userId) || userNames.containsKey(userId);
    }

    @Override
    public Optional<HubUser> findById(UUID userId) {
        if (!existsById(userId)) {
            return Optional.empty();
        }

        return Optional.of(new HubUser(
                userId,
                userNames.getOrDefault(userId, "Usuário mock " + userId),
                userTypes.get(userId)
        ));
    }

    @Override
    public Optional<UserType> findUserTypeById(UUID userId) {
        return Optional.ofNullable(userTypes.get(userId));
    }
}
