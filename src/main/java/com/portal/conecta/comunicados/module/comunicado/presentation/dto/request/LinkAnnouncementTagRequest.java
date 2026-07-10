package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementTag;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LinkAnnouncementTagRequest(

    @NotNull
    UUID announcementId,

    @NotNull
    UUID tagId

) {

    public static LinkAnnouncementTagRequest fromEntity(AnnouncementTag entity) {
        return new LinkAnnouncementTagRequest(
                entity.getAnnouncement() != null ? entity.getAnnouncement().getId() : null,
                entity.getTag() != null ? entity.getTag().getId() : null
        );
    }
}