package com.portal.conecta.comunicados.module.comunicado.application.command;

import java.time.Instant;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.RescheduleAnnouncementRequest;

public record RescheduleAnnouncementCommand(
        UUID announcementId,
        Instant scheduledFor,
        UUID requestingUserId
) {
    public static RescheduleAnnouncementCommand from(
            RescheduleAnnouncementRequest request,
            UUID announcementId,
            UUID requestingUserId
    ) {
        return new RescheduleAnnouncementCommand(announcementId, request.scheduledFor(), requestingUserId);
    }

    public Announcement applyTo(Announcement announcement, Instant now) {
        announcement.setScheduledFor(scheduledFor());
        announcement.setUpdatedAt(now);
        return announcement;
    }
}
