package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;

import java.time.Instant;
import java.util.UUID;

public record AnnouncementSummaryResponse(

    UUID id,
    String title,
    String description,
    AnnouncementOrigin origin,
    AnnouncementStatus status,
    Boolean pinned,
    Short pinnedOrder,
    Instant scheduledFor,
    Instant publishedAt,
    Instant createdAt

) {

    public static AnnouncementSummaryResponse from(Announcement entity) {
        return new AnnouncementSummaryResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getOrigin(),
                entity.getStatus(),
                entity.isPinned(),
                entity.getPinnedOrder(),
                entity.getScheduledFor(),
                entity.getPublishedAt(),
                entity.getCreatedAt()
        );
    }
}