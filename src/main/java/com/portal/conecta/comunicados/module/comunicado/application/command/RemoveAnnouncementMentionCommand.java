package com.portal.conecta.comunicados.module.comunicado.application.command;

import java.util.UUID;

public record RemoveAnnouncementMentionCommand(

    UUID announcementId,
    UUID userId,
    UUID actorUserId

) {}