package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AnnouncementIndividualNoticeResponse(

    UUID id,
    UUID announcementId,
    UUID categoryId,
    Instant resolvedAt

) {}