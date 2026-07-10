package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementDestinationRequest;

public record AddAnnouncementDestinationCommand(

    CreateAnnouncementDestinationRequest data

) {

    public static AddAnnouncementDestinationCommand fromRequest(CreateAnnouncementDestinationRequest request) {
        return new AddAnnouncementDestinationCommand(request);
    }

    public AnnouncementDestination toEntity(Announcement announcement) {
        return AnnouncementDestination.builder()
                .announcement(announcement)
                .type(data.type())
                .referenceId(data.referenceId())
                .build();
    }
}
