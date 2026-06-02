package com.portal.conecta.comunicados.module.comunicado.application.command;

import java.util.UUID;

public record RemoveAnnouncementFileCommand(

    UUID fileId,
    UUID actorUserId
    
) {}