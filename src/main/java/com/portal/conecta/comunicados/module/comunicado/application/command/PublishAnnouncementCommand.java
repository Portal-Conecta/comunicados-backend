package com.portal.conecta.comunicados.module.comunicado.application.command;

import java.util.UUID;

public record PublishAnnouncementCommand(UUID id) {

    public static  PublishAnnouncementCommand from(UUID id) {
        return new  PublishAnnouncementCommand(id);
    }

}