package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementTag;

import java.util.List;
import java.util.UUID;

public record AnnouncementTagResponse(

    UUID announcementId,
    UUID tagId,
    String tagName

) {

    public static AnnouncementTagResponse fromEntity(AnnouncementTag entity) {
        return new AnnouncementTagResponse(
                entity.getAnnouncement() != null ? entity.getAnnouncement().getId() : null,
                entity.getTag() != null ? entity.getTag().getId() : null,
                entity.getTag() != null ? entity.getTag().getName() : null
        );
    }

    public static List<AnnouncementTagResponse> fromEntities(List<AnnouncementTag> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream().map(AnnouncementTagResponse::fromEntity).toList();
    }
}
