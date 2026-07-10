package com.portal.conecta.comunicados.module.tag.infrastructure.messaging.dto;

import java.time.Instant;

public record CoreEntityEventEnvelope(

        String eventId,
        String correlationId,
        String source,
        String eventType,
        Instant occurredAt,
        String entityType,
        String entityId,
        String code,
        String name

) {}