package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementHistoryRequest;

import java.time.Instant;

public record RecordAnnouncementHistoryCommand(

    CreateAnnouncementHistoryRequest data

) {

    public static RecordAnnouncementHistoryCommand fromRequest(CreateAnnouncementHistoryRequest request) {
        return new RecordAnnouncementHistoryCommand(request);
    }

    public static RecordAnnouncementHistoryCommand fromEntity(AnnouncementHistory entity) {
        return new RecordAnnouncementHistoryCommand(CreateAnnouncementHistoryRequest.fromEntity(entity));
    }

    public AnnouncementHistory toEntity(Announcement announcement, Instant now) {
        return AnnouncementHistory.builder()
                .announcement(announcement)
                .userId(data.userId())
                .action(data.action())
                .snapshot(data.snapshot())
                .createdAt(now)
                .build();
    }
}
