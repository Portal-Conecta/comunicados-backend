package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.UpdateAnnouncementRequest;

import java.util.UUID;

public record UpdateAnnouncementCommand(

    UUID id,
    UpdateAnnouncementRequest data

) {

    public static UpdateAnnouncementCommand fromRequest(UUID id, UpdateAnnouncementRequest request) {
        return new UpdateAnnouncementCommand(id, request);
    }

    public static UpdateAnnouncementCommand fromEntity(Announcement entity) {
        return new UpdateAnnouncementCommand(entity.getId(), UpdateAnnouncementRequest.fromEntity(entity));
    }
}
