package com.portal.conecta.comunicados.module.tag.application.command;

import java.util.UUID;

public record UnlinkAnnouncementTagCommand(
        UUID announcementId,
        UUID tagId,
        UUID requestingUserId
) {
    public static UnlinkAnnouncementTagCommand from(
            UUID announcementId,
            UUID tagId,
            UUID requestingUserId
    ) {
        return new UnlinkAnnouncementTagCommand(announcementId, tagId, requestingUserId);
    }
}
