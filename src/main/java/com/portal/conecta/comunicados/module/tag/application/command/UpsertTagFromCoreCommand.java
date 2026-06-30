package com.portal.conecta.comunicados.module.tag.application.command;

import java.time.Instant;

import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.dto.CoreEntityEventEnvelope;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.exception.InvalidCoreEntityEventException;

public record UpsertTagFromCoreCommand(
        String hubEntityId,
        TagEntityType entityType,
        String name,
        boolean active
) {

    public static UpsertTagFromCoreCommand from(CoreEntityEventEnvelope envelope) {
        if (envelope == null) {
            throw new InvalidCoreEntityEventException("envelope do evento é obrigatório.");
        }
        if (envelope.entityId() == null || envelope.entityId().isBlank()) {
            throw new InvalidCoreEntityEventException("entityId é obrigatório.");
        }

        String name = resolveName(envelope);

        return new UpsertTagFromCoreCommand(
                envelope.entityId().trim(),
                resolveEntityType(envelope.entityType()),
                name,
                true
        );
    }

    public Tag toNewEntity(Instant now) {
        return Tag.builder()
                .hubEntityId(hubEntityId)
                .name(name)
                .entityType(entityType)
                .active(active)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void applyTo(Tag existing, Instant now) {
        existing.setName(name);
        existing.setActive(active);
        existing.setUpdatedAt(now);
    }

    private static String resolveName(CoreEntityEventEnvelope envelope) {
        if (envelope.name() != null && !envelope.name().isBlank()) {
            return envelope.name().trim();
        }
        if (envelope.code() != null && !envelope.code().isBlank()) {
            return envelope.code().trim();
        }
        throw new InvalidCoreEntityEventException("name ou code é obrigatório para upsert de tag.");
    }

    private static TagEntityType resolveEntityType(String entityType) {
        if (entityType == null || entityType.isBlank()) {
            throw new InvalidCoreEntityEventException("entityType é obrigatório.");
        }

        return switch (entityType.trim().toLowerCase()) {
            case "course" -> TagEntityType.COURSE;
            case "class" -> TagEntityType.CLASS;
            default -> throw new InvalidCoreEntityEventException("entityType não suportado: " + entityType);
        };
    }

}