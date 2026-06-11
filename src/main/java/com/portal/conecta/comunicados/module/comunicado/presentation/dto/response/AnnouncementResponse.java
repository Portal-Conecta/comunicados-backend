package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;

import java.time.Instant;
import java.util.List;
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

    public static AnnouncementResponse fromEntity(Announcement entity) {
        return new AnnouncementResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getOrigin(),
                entity.getStatus(),
                entity.isPinned(),
                entity.getPinnedOrder(),
                entity.getCreatedByUserId(),
                entity.getPublishedByUserId(),
                entity.getScheduledFor(),
                entity.getPublishedAt(),
                entity.getRemovedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static List<AnnouncementResponse> fromEntities(List<Announcement> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream().map(AnnouncementResponse::fromEntity).toList();
    }
}
