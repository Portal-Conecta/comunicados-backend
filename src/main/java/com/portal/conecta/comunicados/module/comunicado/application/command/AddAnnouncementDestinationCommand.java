package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementDestinationRequest;

public record AddAnnouncementDestinationCommand(

    CreateAnnouncementDestinationRequest data
    
) {}