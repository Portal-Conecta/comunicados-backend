package com.portal.conecta.comunicados.module.tag.infrastructure.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record CoreEntityEventEnvelope(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String source,
        CoreEntityEventPayload payload
) {
}
