package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementIndividualNotice;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnnouncementIndividualNoticeResponse(

    UUID id,
    UUID announcementId,
    UUID categoryId,
    Instant resolvedAt

) {

    public static AnnouncementIndividualNoticeResponse fromEntity(AnnouncementIndividualNotice entity) {
        return new AnnouncementIndividualNoticeResponse(
                entity.getId(),
                entity.getAnnouncement() != null ? entity.getAnnouncement().getId() : null,
                entity.getCategory() != null ? entity.getCategory().getId() : null,
                entity.getResolvedAt()
        );
    }

    public static List<AnnouncementIndividualNoticeResponse> fromEntities(List<AnnouncementIndividualNotice> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream().map(AnnouncementIndividualNoticeResponse::fromEntity).toList();
    }
}
