package com.portal.conecta.comunicados.module.comunicado.application.command;

import java.util.UUID;

public record UnlinkAnnouncementTagCommand(

    UUID announcementId,
    UUID tagId,
    UUID actorUserId

) {

    public static UnlinkAnnouncementTagCommand of(UUID announcementId, UUID tagId, UUID actorUserId) {
        return new UnlinkAnnouncementTagCommand(announcementId, tagId, actorUserId);
    }
}
