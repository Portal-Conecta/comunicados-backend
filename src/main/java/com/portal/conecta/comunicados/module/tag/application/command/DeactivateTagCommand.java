package com.portal.conecta.comunicados.module.tag.application.command;

import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.dto.CoreEntityEventEnvelope;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.exception.InvalidCoreEntityEventException;

public record DeactivateTagCommand(
        String hubEntityId,
        TagEntityType entityType
) {

    public static DeactivateTagCommand from(CoreEntityEventEnvelope envelope) {
        if (envelope == null) {
            throw new InvalidCoreEntityEventException("envelope do evento é obrigatório.");
        }
        if (envelope.entityId() == null || envelope.entityId().isBlank()) {
            throw new InvalidCoreEntityEventException("entityId é obrigatório.");
        }

        return new DeactivateTagCommand(
                envelope.entityId().trim(),
                resolveEntityType(envelope.entityType())
        );
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