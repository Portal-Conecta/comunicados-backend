package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;

public record AnnouncementDestinationResponse(

    UUID id,
    UUID announcementId,
    AnnouncementDestinationType type,
    UUID referenceId

) {

    public static AnnouncementDestinationResponse from(AnnouncementDestination entity) {
        return new AnnouncementDestinationResponse(
                entity.getId(),
                entity.getAnnouncement().getId(),
                entity.getType(),
                entity.getReferenceId()
        );
    }
}