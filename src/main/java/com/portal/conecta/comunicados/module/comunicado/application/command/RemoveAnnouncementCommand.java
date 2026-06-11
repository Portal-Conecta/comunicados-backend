package com.portal.conecta.comunicados.module.comunicado.application.command;

import java.util.UUID;

public record RemoveAnnouncementCommand(

    UUID id,
    UUID removedByUserId

) {

    public static RemoveAnnouncementCommand of(UUID id, UUID removedByUserId) {
        return new RemoveAnnouncementCommand(id, removedByUserId);
    }
}
