package com.portal.conecta.comunicados.module.comunicado.application.command;

import java.time.Instant;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.PublishAnnouncementRequest;

public record PublishAnnouncementCommand(UUID id) {

    public static  PublishAnnouncementCommand from(UUID id) {
        return new  PublishAnnouncementCommand(id);
    }

    public static PublishAnnouncementCommand fromRequest(
            UUID id,
            PublishAnnouncementRequest request,
            UUID publishedByUserId
    ) {
        return new PublishAnnouncementCommand(id);
    }

    public Announcement toEntity(Announcement existing, Instant now) {
        existing.setStatus(AnnouncementStatus.PUBLISHED);
        existing.setPublishedAt(now);
        existing.setPublishedByUserId(existing.getPublishedByUserId());
        existing.setUpdatedAt(now);
        return existing;
    }
  
}