package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.PublishAnnouncementRequest;

import java.util.UUID;

public record PublishAnnouncementCommand(

        UUID id,
        PublishAnnouncementRequest data,
        UUID publishedByUserId

) {}