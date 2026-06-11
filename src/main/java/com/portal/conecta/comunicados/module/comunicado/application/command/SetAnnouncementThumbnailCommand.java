package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;

import java.util.UUID;

public record SetAnnouncementThumbnailCommand(

    UUID announcementId,
    UUID fileId,
    UUID actorUserId

) {

    public static SetAnnouncementThumbnailCommand of(UUID announcementId, UUID fileId, UUID actorUserId) {
        return new SetAnnouncementThumbnailCommand(announcementId, fileId, actorUserId);
    }

    public AnnouncementFile toEntity(AnnouncementFile existing) {
        existing.setThumbnail(true);
        return existing;
    }
}
