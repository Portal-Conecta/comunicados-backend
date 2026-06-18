package com.portal.conecta.comunicados.module.tag.infrastructure.messaging.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;

public record CoreEntityEventPayload(
        UUID entityId,
        TagEntityType entityType,
        String name,
        Boolean active,
        Map<String, Object> metadata
) {

    public boolean resolvedActive() {
        return active == null || active;
    }
}
