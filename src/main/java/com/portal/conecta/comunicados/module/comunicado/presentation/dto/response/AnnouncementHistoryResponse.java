package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnnouncementHistoryResponse(

    UUID id,
    UUID announcementId,
    UUID userId,
    AnnouncementHistoryAction action,
    String snapshot,
    Instant createdAt

) {

    public static AnnouncementHistoryResponse fromEntity(AnnouncementHistory entity) {
        return new AnnouncementHistoryResponse(
                entity.getId(),
                entity.getAnnouncement() != null ? entity.getAnnouncement().getId() : null,
                entity.getUserId(),
                entity.getAction(),
                entity.getSnapshot(),
                entity.getCreatedAt()
        );
    }

    public static List<AnnouncementHistoryResponse> fromEntities(List<AnnouncementHistory> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream().map(AnnouncementHistoryResponse::fromEntity).toList();
    }
}
