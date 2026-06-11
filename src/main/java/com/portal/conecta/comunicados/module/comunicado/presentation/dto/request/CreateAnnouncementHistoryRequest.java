package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;

import jakarta.validation.constraints.NotNull;

public record CreateAnnouncementHistoryRequest(

    @NotNull
    UUID announcementId,

    @NotNull
    UUID userId,

    @NotNull
    AnnouncementHistoryAction action,

    String snapshot

) {

    public static CreateAnnouncementHistoryRequest fromEntity(AnnouncementHistory entity) {
        return new CreateAnnouncementHistoryRequest(
                entity.getAnnouncement() != null ? entity.getAnnouncement().getId() : null,
                entity.getUserId(),
                entity.getAction(),
                entity.getSnapshot()
        );
    }
}