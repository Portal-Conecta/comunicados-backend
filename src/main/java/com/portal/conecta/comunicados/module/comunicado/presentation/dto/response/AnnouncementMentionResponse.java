package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementMention;

public record AnnouncementMentionResponse(

    UUID announcementId,
    UUID userId

) {

    public static AnnouncementMentionResponse from(AnnouncementMention entity) {
        return new AnnouncementMentionResponse(
                entity.getAnnouncement().getId(),
                entity.getUserId()
        );
    }
}