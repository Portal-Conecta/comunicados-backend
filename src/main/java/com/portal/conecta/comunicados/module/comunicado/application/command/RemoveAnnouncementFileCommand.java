package com.portal.conecta.comunicados.module.comunicado.application.command;

import java.util.UUID;

public record RemoveAnnouncementFileCommand(

    UUID fileId,
    UUID actorUserId

) {

    public static RemoveAnnouncementFileCommand of(UUID fileId, UUID actorUserId) {
        return new RemoveAnnouncementFileCommand(fileId, actorUserId);
    }
}
