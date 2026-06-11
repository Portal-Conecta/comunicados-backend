package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.PublishAnnouncementRequest;

import java.util.UUID;

public record PublishAnnouncementCommand(UUID id) {

    public static  PublishAnnouncementCommand from(UUID id) {
        return new  PublishAnnouncementCommand(id);
    }

}