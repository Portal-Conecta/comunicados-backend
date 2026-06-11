package com.portal.conecta.comunicados.module.comunicado.application.command;

import java.util.UUID;

public record SetAnnouncementThumbnailCommand(

    UUID announcementId,
    UUID fileId,
    UUID actorUserId

) {

    public static SetAnnouncementThumbnailCommand of(UUID announcementId, UUID fileId, UUID actorUserId) {
        return new SetAnnouncementThumbnailCommand(announcementId, fileId, actorUserId);
    }
}
