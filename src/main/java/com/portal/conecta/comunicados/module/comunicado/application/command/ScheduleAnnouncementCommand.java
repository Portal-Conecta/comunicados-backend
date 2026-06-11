package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.ScheduleAnnouncementRequest;

import java.time.Instant;
import java.util.UUID;

public record ScheduleAnnouncementCommand(

    UUID id,
    ScheduleAnnouncementRequest data

) {

    public static ScheduleAnnouncementCommand fromRequest(UUID id, ScheduleAnnouncementRequest request) {
        return new ScheduleAnnouncementCommand(id, request);
    }

    public Announcement toEntity(Announcement existing, Instant now) {
        existing.setScheduledFor(data.scheduledFor());
        existing.setStatus(AnnouncementStatus.SCHEDULED);
        existing.setUpdatedAt(now);
        return existing;
    }
}
