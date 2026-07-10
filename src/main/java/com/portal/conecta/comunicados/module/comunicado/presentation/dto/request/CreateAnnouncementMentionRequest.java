package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementMention;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAnnouncementMentionRequest(

    @NotNull
    UUID announcementId,

    @NotNull
    UUID userId

) {

    public static CreateAnnouncementMentionRequest fromEntity(AnnouncementMention entity) {
        return new CreateAnnouncementMentionRequest(
                entity.getAnnouncement() != null ? entity.getAnnouncement().getId() : null,
                entity.getUserId()
        );
    }
}