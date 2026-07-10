package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementDestinationRequest;

import java.util.List;
import java.util.UUID;

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

    public List<AnnouncementDestination> toEntities(Announcement announcement) {
        return destinations.stream()
                .map(destination -> AnnouncementDestination.builder()
                        .announcement(announcement)
                        .type(destination.type())
                        .referenceId(destination.referenceId())
                        .build())
                .toList();
    }
}
