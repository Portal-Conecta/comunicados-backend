package com.portal.conecta.comunicados.module.comunicado.application.command;

import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementFileRequest;

public record AttachAnnouncementFileCommand(

    CreateAnnouncementFileRequest data,
    UUID uploadedByUserId

) {

    public static AttachAnnouncementFileCommand fromRequest(
            CreateAnnouncementFileRequest request,
            UUID uploadedByUserId
    ) {
        return new AttachAnnouncementFileCommand(request, uploadedByUserId);
    }
}
