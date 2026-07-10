package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AnnouncementSummaryResponse(

    UUID id,
    @Schema(description = "Título do comunicado.", maxLength = 255)
    String title,
    @Schema(description = "Conteúdo do comunicado em texto livre, sem limite de tamanho.")
    String description,
    AnnouncementOrigin origin,
    AnnouncementStatus status,
    Boolean pinned,
    Short pinnedOrder,
    Instant scheduledFor,
    Instant publishedAt,
    Instant createdAt,
    @Schema(description = "URL pré-assinada da miniatura do comunicado, se houver e estiver pronta.")
    String thumbnailUrl

) {

    public static AnnouncementSummaryResponse fromEntity(Announcement entity) {
        return fromEntity(entity, null);
    }

    public static AnnouncementSummaryResponse fromEntity(Announcement entity, String thumbnailUrl) {
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
                entity.getCreatedAt(),
                thumbnailUrl
        );
    }

    public static List<AnnouncementSummaryResponse> fromEntities(List<Announcement> entities) {
        return fromEntities(entities, Map.of());
    }

    public static List<AnnouncementSummaryResponse> fromEntities(
            List<Announcement> entities,
            Map<UUID, String> thumbnailUrlsByAnnouncementId
    ) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> urls = thumbnailUrlsByAnnouncementId != null
                ? thumbnailUrlsByAnnouncementId
                : Map.of();
        return entities.stream()
                .map(entity -> fromEntity(entity, urls.get(entity.getId())))
                .toList();
    }
}
