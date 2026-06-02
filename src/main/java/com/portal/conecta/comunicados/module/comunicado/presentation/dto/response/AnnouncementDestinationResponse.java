package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;

public record AnnouncementDestinationResponse(

    UUID id,
    UUID announcementId,
    AnnouncementDestinationType type,
    UUID referenceId

) {}