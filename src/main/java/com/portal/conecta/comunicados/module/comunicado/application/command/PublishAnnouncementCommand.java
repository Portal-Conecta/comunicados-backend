package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.PublishAnnouncementRequest;

import java.time.Instant;
import java.util.UUID;

public record PublishAnnouncementCommand(

        UUID id,
        PublishAnnouncementRequest data,
        UUID publishedByUserId

) {

    public static PublishAnnouncementCommand fromRequest(
            UUID id,
            PublishAnnouncementRequest request,
            UUID publishedByUserId
    ) {
        return new PublishAnnouncementCommand(id, request, publishedByUserId);
    }

    public Announcement toEntity(Announcement existing, Instant now) {
        existing.setStatus(AnnouncementStatus.PUBLISHED);
        existing.setPublishedAt(now);
        existing.setPublishedByUserId(publishedByUserId);
        existing.setUpdatedAt(now);
        return existing;
    }
}
