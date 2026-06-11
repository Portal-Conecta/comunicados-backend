package com.portal.conecta.comunicados.module.comunicado.application.command;

import java.util.UUID;

public record RemoveAnnouncementDestinationCommand(

    UUID destinationId,
    UUID actorUserId

) {

    public static RemoveAnnouncementDestinationCommand of(UUID destinationId, UUID actorUserId) {
        return new RemoveAnnouncementDestinationCommand(destinationId, actorUserId);
    }
}
