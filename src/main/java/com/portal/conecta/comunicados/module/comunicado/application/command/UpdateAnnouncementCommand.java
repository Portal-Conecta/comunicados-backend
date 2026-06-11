package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.UpdateAnnouncementRequest;

import java.time.Instant;
import java.util.UUID;

public record UpdateAnnouncementCommand(

    UUID id,
    UpdateAnnouncementRequest data

) {

    public static UpdateAnnouncementCommand fromRequest(UUID id, UpdateAnnouncementRequest request) {
        return new UpdateAnnouncementCommand(id, request);
    }

    public static UpdateAnnouncementCommand fromEntity(Announcement entity) {
        return new UpdateAnnouncementCommand(entity.getId(), UpdateAnnouncementRequest.fromEntity(entity));
    }

    public Announcement toEntity(Announcement existing, Instant now) {
        if (data.title() != null) {
            existing.setTitle(data.title());
        }
        if (data.description() != null) {
            existing.setDescription(data.description());
        }
        if (data.origin() != null) {
            existing.setOrigin(data.origin());
        }
        if (data.status() != null) {
            existing.setStatus(data.status());
        }
        if (data.pinned() != null) {
            existing.setPinned(data.pinned());
        }
        if (data.pinnedOrder() != null) {
            existing.setPinnedOrder(data.pinnedOrder());
        }
        if (data.scheduledFor() != null) {
            existing.setScheduledFor(data.scheduledFor());
        }
        existing.setUpdatedAt(now);
        return existing;
    }
}
