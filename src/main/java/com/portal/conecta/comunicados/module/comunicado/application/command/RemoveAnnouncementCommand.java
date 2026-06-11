package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;

import java.time.Instant;
import java.util.UUID;

public record RemoveAnnouncementCommand(

    UUID id,
    UUID removedByUserId

) {

    public static RemoveAnnouncementCommand of(UUID id, UUID removedByUserId) {
        return new RemoveAnnouncementCommand(id, removedByUserId);
    }

    public Announcement toEntity(Announcement existing, Instant now) {
        existing.setStatus(AnnouncementStatus.REMOVED);
        existing.setRemovedAt(now);
        existing.setUpdatedAt(now);
        return existing;
    }

    public AnnouncementHistory toRemovalHistory(Announcement announcement, Instant now) {
        return AnnouncementHistory.builder()
                .announcement(announcement)
                .userId(removedByUserId)
                .action(AnnouncementHistoryAction.REMOVAL)
                .createdAt(now)
                .build();
    }
}
