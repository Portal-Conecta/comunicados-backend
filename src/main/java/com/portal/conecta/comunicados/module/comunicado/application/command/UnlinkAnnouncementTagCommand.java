package com.portal.conecta.comunicados.module.comunicado.application.command;

import java.util.UUID;

public record UnlinkAnnouncementTagCommand(

    UUID announcementId,
    UUID tagId,
    UUID actorUserId

) {}