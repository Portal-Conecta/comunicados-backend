package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementIndividualNotice;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAnnouncementIndividualNoticeRequest(

    @NotNull
    UUID announcementId,

    @NotNull
    UUID categoryId

) {

    public static CreateAnnouncementIndividualNoticeRequest fromEntity(AnnouncementIndividualNotice entity) {
        return new CreateAnnouncementIndividualNoticeRequest(
                entity.getAnnouncement() != null ? entity.getAnnouncement().getId() : null,
                entity.getCategory() != null ? entity.getCategory().getId() : null
        );
    }
}