package com.portal.conecta.comunicados.module.tag.application.command;

import java.util.UUID;

import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.dto.CoreEntityEventEnvelope;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.dto.CoreEntityEventPayload;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.exception.InvalidCoreEntityEventException;

public record DeactivateTagCommand(
        UUID hubEntityId,
        TagEntityType entityType
) {

    public static DeactivateTagCommand from(CoreEntityEventEnvelope envelope) {
        CoreEntityEventPayload payload = requirePayload(envelope);

        if (payload.entityId() == null) {
            throw new InvalidCoreEntityEventException("payload.entityId é obrigatório.");
        }
        if (payload.entityType() == null) {
            throw new InvalidCoreEntityEventException("payload.entityType é obrigatório.");
        }

        return new DeactivateTagCommand(payload.entityId(), payload.entityType());
    }

    private static CoreEntityEventPayload requirePayload(CoreEntityEventEnvelope envelope) {
        if (envelope == null || envelope.payload() == null) {
            throw new InvalidCoreEntityEventException("payload do evento é obrigatório.");
        }
        return envelope.payload();
    }
}
