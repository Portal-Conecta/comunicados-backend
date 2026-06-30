package com.portal.conecta.comunicados.module.comunicado.infrastructure.messaging.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AnnouncementNotificationPayload(
        String messageId,
        String correlationId,
        String source,
        String eventType,
        Instant occurredAt,
        String title,
        String body,
        List<NotificationScopePayload> scope,
        List<NotificationFilterPayload> filters,
        Map<String, String> metadata
) {

    public record NotificationScopePayload(
            String type,
            String correlationId
    ) {}

    public record NotificationFilterPayload(
            String type,
            String value
    ) {}
}
