package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;

import java.util.List;
import java.util.UUID;

public record AnnouncementDestinationResponse(

    UUID id,
    UUID announcementId,
    AnnouncementDestinationType type,
    UUID referenceId

) {

    public static AnnouncementDestinationResponse fromEntity(AnnouncementDestination entity) {
        return new AnnouncementDestinationResponse(
                entity.getId(),
                entity.getAnnouncement() != null ? entity.getAnnouncement().getId() : null,
                entity.getType(),
                entity.getReferenceId()
        );
    }

    public static List<AnnouncementDestinationResponse> fromEntities(List<AnnouncementDestination> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream().map(AnnouncementDestinationResponse::fromEntity).toList();
    }
}
