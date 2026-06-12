package com.portal.conecta.comunicados.module.comunicado.application.command;

import java.time.Instant;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.ScheduleAnnouncementRequest;

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

    public AnnouncementHistory toHistory(Announcement announcement, UUID userId, Instant now) {
        return AnnouncementHistory.builder()
                .announcement(announcement)
                .userId(userId)
                .action(AnnouncementHistoryAction.SCHEDULED)
                .snapshot(createSnapshot(announcement))
                .createdAt(now)
                .build();
    }

    private String createSnapshot(Announcement announcement) {
        return "Comunicado agendado: "
                + announcement.getTitle()
                + " | Agendado para: "
                + data.scheduledFor();
    }
}
