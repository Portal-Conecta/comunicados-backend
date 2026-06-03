package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAnnouncementMentionRequest(

    @NotNull
    UUID announcementId,

    @NotNull
    UUID userId

) {}