package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;

import java.time.Instant;
import java.util.UUID;

public record AnnouncementHistoryResponse(

    UUID id,
    UUID announcementId,
    UUID userId,
    AnnouncementHistoryAction action,
    String snapshot,
    Instant createdAt

) {}