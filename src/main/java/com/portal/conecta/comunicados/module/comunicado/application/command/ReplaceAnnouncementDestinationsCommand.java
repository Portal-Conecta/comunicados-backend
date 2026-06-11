package com.portal.conecta.comunicados.module.comunicado.application.command;

import java.util.List;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementDestinationRequest;

public record ReplaceAnnouncementDestinationsCommand(

    UUID announcementId,
    List<CreateAnnouncementDestinationRequest> destinations,
    UUID actorUserId

) {

    public static ReplaceAnnouncementDestinationsCommand fromRequest(
            UUID announcementId,
            List<CreateAnnouncementDestinationRequest> destinations,
            UUID actorUserId
    ) {
        return new ReplaceAnnouncementDestinationsCommand(announcementId, destinations, actorUserId);
    }
}
