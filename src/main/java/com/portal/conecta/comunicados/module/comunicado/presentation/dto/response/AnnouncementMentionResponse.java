package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import java.util.UUID;

public record AnnouncementMentionResponse(

    UUID announcementId,
    UUID userId

) {}