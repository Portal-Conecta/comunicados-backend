package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;

import jakarta.validation.constraints.NotNull;

public record CreateAnnouncementHistoryRequest(

    @NotNull
    UUID announcementId,

    @NotNull
    UUID userId,

    @NotNull
    AnnouncementHistoryAction action,

    String snapshot

) {}