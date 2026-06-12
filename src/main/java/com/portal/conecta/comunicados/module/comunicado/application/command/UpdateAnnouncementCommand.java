package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.UpdateAnnouncementRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UpdateAnnouncementCommand(
        UUID id,
        UpdateAnnouncementRequest data,
        UUID updatedByUserId
) {

    public static UpdateAnnouncementCommand fromRequest(
            UUID id,
            UpdateAnnouncementRequest request,
            UUID updatedByUserId
    ) {
        return new UpdateAnnouncementCommand(id, request, updatedByUserId);
    }

    public static UpdateAnnouncementCommand fromEntity(Announcement entity) {
        return new UpdateAnnouncementCommand(
                entity.getId(),
                UpdateAnnouncementRequest.fromEntity(entity),
                entity.getCreatedByUserId()
        );
    }

    public Announcement toEntity(Announcement existing, Instant now) {
        if (data.title() != null) existing.setTitle(data.title());
        if (data.description() != null) existing.setDescription(data.description());
        if (data.origin() != null) existing.setOrigin(data.origin());
        if (data.status() != null) existing.setStatus(data.status());
        if (data.pinned() != null) existing.setPinned(data.pinned());
        if (data.pinnedOrder() != null) existing.setPinnedOrder(data.pinnedOrder());
        if (data.scheduledFor() != null) existing.setScheduledFor(data.scheduledFor());

        existing.setUpdatedAt(now);
        return existing;
    }

    public List<AnnouncementDestination> toDestinations(Announcement announcement) {
        if (data.destinations() == null) {
            return List.of();
        }

        return data.destinations().stream()
                .map(destination -> AnnouncementDestination.builder()
                        .announcement(announcement)
                        .type(destination.type())
                        .referenceId(destination.referenceId())
                        .build())
                .toList();
    }

    public AnnouncementHistory toEditHistory(Announcement announcement, Instant now, String snapshot) {
        return AnnouncementHistory.builder()
                .announcement(announcement)
                .userId(updatedByUserId)
                .action(AnnouncementHistoryAction.EDIT)
                .snapshot(snapshot)
                .createdAt(now)
                .build();
    }

    public String toSnapshot(
            Announcement announcement,
            List<AnnouncementDestination> destinations,
            ObjectMapper objectMapper
    ) {
        try {
            return objectMapper.writeValueAsString(AnnouncementSnapshot.from(announcement, destinations));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize announcement snapshot.", exception);
        }
    }

    private record AnnouncementSnapshot(
            UUID id,
            String title,
            String description,
            AnnouncementOrigin origin,
            AnnouncementStatus status,
            boolean pinned,
            Short pinnedOrder,
            UUID createdByUserId,
            UUID publishedByUserId,
            String scheduledFor,
            String publishedAt,
            String removedAt,
            String createdAt,
            String updatedAt,
            List<DestinationSnapshot> destinations
    ) {
        private static AnnouncementSnapshot from(
                Announcement announcement,
                List<AnnouncementDestination> destinations
        ) {
            return new AnnouncementSnapshot(
                    announcement.getId(),
                    announcement.getTitle(),
                    announcement.getDescription(),
                    announcement.getOrigin(),
                    announcement.getStatus(),
                    announcement.isPinned(),
                    announcement.getPinnedOrder(),
                    announcement.getCreatedByUserId(),
                    announcement.getPublishedByUserId(),
                    format(announcement.getScheduledFor()),
                    format(announcement.getPublishedAt()),
                    format(announcement.getRemovedAt()),
                    format(announcement.getCreatedAt()),
                    format(announcement.getUpdatedAt()),
                    DestinationSnapshot.fromEntities(destinations)
            );
        }

        private static String format(Instant instant) {
            return instant == null ? null : instant.toString();
        }
    }

    private record DestinationSnapshot(
            UUID id,
            AnnouncementDestinationType type,
            UUID referenceId
    ) {
        private static List<DestinationSnapshot> fromEntities(List<AnnouncementDestination> destinations) {
            if (destinations == null || destinations.isEmpty()) {
                return List.of();
            }

            return destinations.stream()
                    .map(destination -> new DestinationSnapshot(
                            destination.getId(),
                            destination.getType(),
                            destination.getReferenceId()
                    ))
                    .toList();
        }
    }

}