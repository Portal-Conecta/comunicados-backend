package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateAnnouncementCommand(

        CreateAnnouncementRequest data,
        UUID createdByUserId

) {

    public static CreateAnnouncementCommand fromRequest(CreateAnnouncementRequest request, UUID createdByUserId) {
        return new CreateAnnouncementCommand(request, createdByUserId);
    }

    public Announcement toEntity(Instant now) {
        return Announcement.builder()
                .title(data.title())
                .description(data.description())
                .origin(data.origin())
                .status(data.status())
                .pinned(data.pinned() != null && data.pinned())
                .pinnedOrder(data.pinnedOrder())
                .scheduledFor(data.scheduledFor())
                .createdByUserId(createdByUserId)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public List<AnnouncementDestination> toDestinations(Announcement announcement) {
        return data.destinations().stream()
                .map(destination -> AnnouncementDestination.builder()
                        .announcement(announcement)
                        .type(destination.type())
                        .referenceId(destination.referenceId())
                        .build())
                .toList();
    }

    public AnnouncementHistory toCreationHistory(Announcement announcement, Instant now) {
        return AnnouncementHistory.builder()
                .announcement(announcement)
                .action(AnnouncementHistoryAction.CREATION)
                .userId(createdByUserId)
                .createdAt(now)
                .build();
    }
}
