package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;

import io.swagger.v3.oas.annotations.media.Schema;

public record AnnouncementResponse(

    UUID id,
    @Schema(description = "Título do comunicado.", maxLength = 255)
    String title,
    @Schema(description = "Conteúdo do comunicado em HTML sanitizado (TipTap).")
    String description,
    @Schema(description = "Versão plain-text da descrição, sem HTML — gerada no servidor.")
    String descriptionPlain,
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
                entity.getDescriptionPlain(),
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
