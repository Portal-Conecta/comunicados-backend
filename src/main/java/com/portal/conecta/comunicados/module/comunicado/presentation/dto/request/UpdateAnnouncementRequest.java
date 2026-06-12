package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import java.time.Instant;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateAnnouncementRequest(

    @Size(max = 255)
    String title,
    String description,
    AnnouncementOrigin origin,
    Boolean pinned,
    @Min(0)
    Short pinnedOrder,
    Instant scheduledFor

) {

    public static UpdateAnnouncementRequest fromEntity(Announcement entity) {
        return new UpdateAnnouncementRequest(
                entity.getTitle(),
                entity.getDescription(),
                entity.getOrigin(),
                entity.isPinned(),
                entity.getPinnedOrder(),
                entity.getScheduledFor()
        );
    }
}