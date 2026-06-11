package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementMention;

import java.util.List;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementMention;

public record AnnouncementMentionResponse(

    UUID announcementId,
    UUID userId

) {

    public static AnnouncementMentionResponse fromEntity(AnnouncementMention entity) {
        return new AnnouncementMentionResponse(
                entity.getAnnouncement() != null ? entity.getAnnouncement().getId() : null,
                entity.getUserId()
        );
    }

    public static List<AnnouncementMentionResponse> fromEntities(List<AnnouncementMention> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream().map(AnnouncementMentionResponse::fromEntity).toList();
    }
}
