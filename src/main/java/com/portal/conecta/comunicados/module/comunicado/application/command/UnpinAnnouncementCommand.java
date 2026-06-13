package com.portal.conecta.comunicados.module.comunicado.application.command;

import java.util.UUID;

public record UnpinAnnouncementCommand(
        UUID announcementId,
        UUID unpinnedByUserId
) {

    public static  UnpinAnnouncementCommand from(UUID announcementId, UUID unpinnedByUserId) {
        return new UnpinAnnouncementCommand(announcementId, unpinnedByUserId);
    }
}