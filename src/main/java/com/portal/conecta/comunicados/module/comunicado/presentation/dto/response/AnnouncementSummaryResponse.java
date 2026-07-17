package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.presentation.dto.response.TagResponse;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AnnouncementSummaryResponse(

    UUID id,
    @Schema(description = "Título do comunicado.", maxLength = 255)
    String title,
    @Schema(description = "Prévia plain-text da descrição (sem HTML) para mural/cards. "
            + "Compatível: mesmo valor de descriptionPlain.")
    String description,
    @Schema(description = "Versão plain-text da descrição, sem HTML — uso recomendado no mural.")
    String descriptionPlain,
    AnnouncementOrigin origin,
    AnnouncementStatus status,
    Boolean pinned,
    Short pinnedOrder,
    Instant scheduledFor,
    Instant publishedAt,
    Instant createdAt,
    @Schema(description = "URL pré-assinada da miniatura do comunicado, se houver e estiver pronta.")
    String thumbnailUrl,
    @Schema(description = "Tags vinculadas ao comunicado (curso, turma, turno, etc.).")
    List<TagResponse> tags

) {

    public static AnnouncementSummaryResponse fromEntity(Announcement entity) {
        return fromEntity(entity, null, List.of());
    }

    public static AnnouncementSummaryResponse fromEntity(
            Announcement entity,
            String thumbnailUrl,
            List<Tag> tags
    ) {
        String plain = entity.getDescriptionPlain() != null ? entity.getDescriptionPlain() : "";
        return new AnnouncementSummaryResponse(
                entity.getId(),
                entity.getTitle(),
                plain,
                plain,
                entity.getOrigin(),
                entity.getStatus(),
                entity.isPinned(),
                entity.getPinnedOrder(),
                entity.getScheduledFor(),
                entity.getPublishedAt(),
                entity.getCreatedAt(),
                thumbnailUrl,
                TagResponse.fromEntities(tags)
        );
    }

    public static List<AnnouncementSummaryResponse> fromEntities(List<Announcement> entities) {
        return fromEntities(entities, Map.of(), Map.of());
    }

    public static List<AnnouncementSummaryResponse> fromEntities(
            List<Announcement> entities,
            Map<UUID, String> thumbnailUrlsByAnnouncementId,
            Map<UUID, List<Tag>> tagsByAnnouncementId
    ) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> urls = thumbnailUrlsByAnnouncementId != null
                ? thumbnailUrlsByAnnouncementId
                : Map.of();
        Map<UUID, List<Tag>> tags = tagsByAnnouncementId != null
                ? tagsByAnnouncementId
                : Map.of();
        return entities.stream()
                .map(entity -> fromEntity(
                        entity,
                        urls.get(entity.getId()),
                        tags.getOrDefault(entity.getId(), List.of())
                ))
                .toList();
    }
}
