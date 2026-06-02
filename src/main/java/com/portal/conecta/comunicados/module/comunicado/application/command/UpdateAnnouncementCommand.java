package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.UpdateAnnouncementRequest;

import java.util.UUID;

public record UpdateAnnouncementCommand(

    UUID id,
    UpdateAnnouncementRequest data

) {}