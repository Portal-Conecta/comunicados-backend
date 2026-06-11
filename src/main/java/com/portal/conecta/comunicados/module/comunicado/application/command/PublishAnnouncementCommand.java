package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.PublishAnnouncementRequest;

import java.util.UUID;

public record PublishAnnouncementCommand(

        UUID id,
        PublishAnnouncementRequest data,
        UUID publishedByUserId

) {

    public static PublishAnnouncementCommand fromRequest(
            UUID id,
            PublishAnnouncementRequest request,
            UUID publishedByUserId
    ) {
        return new PublishAnnouncementCommand(id, request, publishedByUserId);
    }
}
