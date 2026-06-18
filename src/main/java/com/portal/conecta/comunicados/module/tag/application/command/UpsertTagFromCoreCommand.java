package com.portal.conecta.comunicados.module.tag.application.command;

import java.time.Instant;
import java.util.UUID;

import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.dto.CoreEntityEventEnvelope;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.dto.CoreEntityEventPayload;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.exception.InvalidCoreEntityEventException;

public record UpsertTagFromCoreCommand(
        UUID hubEntityId,
        TagEntityType entityType,
        String name,
        boolean active
) {

    public static UpsertTagFromCoreCommand from(CoreEntityEventEnvelope envelope) {
        CoreEntityEventPayload payload = requirePayload(envelope);

        if (payload.entityId() == null) {
            throw new InvalidCoreEntityEventException("payload.entityId é obrigatório.");
        }
        if (payload.entityType() == null) {
            throw new InvalidCoreEntityEventException("payload.entityType é obrigatório.");
        }
        if (payload.name() == null || payload.name().isBlank()) {
            throw new InvalidCoreEntityEventException("payload.name é obrigatório para upsert de tag.");
        }

        return new UpsertTagFromCoreCommand(
                payload.entityId(),
                payload.entityType(),
                payload.name().trim(),
                payload.resolvedActive()
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

    private static CoreEntityEventPayload requirePayload(CoreEntityEventEnvelope envelope) {
        if (envelope == null || envelope.payload() == null) {
            throw new InvalidCoreEntityEventException("payload do evento é obrigatório.");
        }
        return envelope.payload();
    }
}
