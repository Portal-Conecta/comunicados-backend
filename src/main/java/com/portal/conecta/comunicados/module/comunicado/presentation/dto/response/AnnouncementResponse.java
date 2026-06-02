package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;

import java.time.Instant;
import java.util.UUID;


public record AnnouncementResponse(
    
    UUID id,
    String title,
    String description,
    AnnouncementOrigin origin,
    AnnouncementStatus status,
    Boolean pinned,
    Short pinnedOrder,
    UUID createdByUserId,
    UUID publishedByUserId,
    Instant scheduledFor,
    Instant publishedAt,
    Instant removedAt,
    Instant createdAt,
    Instant updatedAt

) {}