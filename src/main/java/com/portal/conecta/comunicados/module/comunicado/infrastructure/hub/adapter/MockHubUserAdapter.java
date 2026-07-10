package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.adapter;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
import com.portal.conecta.comunicados.shared.context.ContextClass;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.UserType;

@Component
@ConditionalOnProperty(prefix = "hub.api", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockHubUserAdapter implements HubUserPort {

    private final Set<UUID> userIds;
    private final Map<UUID, String> userNames;
    private final Map<UUID, UserType> userTypes;
    private final Map<String, List<HubMockProperties.MockStudent>> studentsByClass;

    public MockHubUserAdapter(HubMockProperties properties) {
        this.userIds = properties.userIds().stream()
                .map(UUID::fromString)
                .collect(Collectors.toUnmodifiableSet());

        this.studentsByClass = properties.studentsByClass();

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

    @Override
    public List<UUID> findUserIdsByNameContaining(String term, RequestContext context) {
        if (term == null || term.isBlank()) {
            return List.of();
        }

        String normalized = term.trim().toLowerCase(Locale.ROOT);
        Set<UUID> scopedUserIds = resolveScopedUserIds(context);

        return scopedUserIds.stream()
                .filter(userId -> {
                    String name = userNames.get(userId);
                    return name != null && name.toLowerCase(Locale.ROOT).contains(normalized);
                })
                .toList();
    }

    private Set<UUID> resolveScopedUserIds(RequestContext context) {
        if (context == null || context.classes() == null || context.classes().isEmpty()) {
            Set<UUID> all = new HashSet<>(userNames.keySet());
            all.addAll(userIds);
            return all;
        }

        Set<UUID> scoped = new HashSet<>();
        for (ContextClass contextClass : context.classes()) {
            List<HubMockProperties.MockStudent> students =
                    studentsByClass.get(contextClass.classId().toString());
            if (students == null) {
                continue;
            }
            for (HubMockProperties.MockStudent student : students) {
                scoped.add(UUID.fromString(student.id()));
            }
        }
        return scoped;
    }
}
