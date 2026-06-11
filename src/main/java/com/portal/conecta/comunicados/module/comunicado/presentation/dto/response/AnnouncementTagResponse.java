package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementTag;

public record AnnouncementTagResponse(

    UUID announcementId,
    UUID tagId,
    String tagName

) {

    public static AnnouncementTagResponse from(AnnouncementTag entity) {
        return new AnnouncementTagResponse(
                entity.getAnnouncement().getId(),
                entity.getTag().getId(),
                entity.getTag().getName()
        );
    }
}