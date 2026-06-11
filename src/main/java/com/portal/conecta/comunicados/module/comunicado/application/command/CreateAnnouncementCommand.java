package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementRequest;

import java.util.UUID;

public record CreateAnnouncementCommand(

        CreateAnnouncementRequest data,
        UUID createdByUserId

) {

    public static CreateAnnouncementCommand fromRequest(CreateAnnouncementRequest request, UUID createdByUserId) {
        return new CreateAnnouncementCommand(request, createdByUserId);
    }
}
