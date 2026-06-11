package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
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

) {

    public static AnnouncementResponse from(Announcement announcement) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getDescription(),
                announcement.getOrigin(),
                announcement.getStatus(),
                announcement.isPinned(),
                announcement.getPinnedOrder(),
                announcement.getCreatedByUserId(),
                announcement.getPublishedByUserId(),
                announcement.getScheduledFor(),
                announcement.getPublishedAt(),
                announcement.getRemovedAt(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }

}