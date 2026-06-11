package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementIndividualNotice;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.ResolveAnnouncementIndividualNoticeRequest;

import java.time.Instant;
import java.util.UUID;

public record ResolveAnnouncementIndividualNoticeCommand(

    UUID id,
    ResolveAnnouncementIndividualNoticeRequest data,
    UUID actorUserId

) {

    public static ResolveAnnouncementIndividualNoticeCommand fromRequest(
            UUID id,
            ResolveAnnouncementIndividualNoticeRequest request,
            UUID actorUserId
    ) {
        return new ResolveAnnouncementIndividualNoticeCommand(id, request, actorUserId);
    }

    public AnnouncementIndividualNotice toEntity(AnnouncementIndividualNotice existing, Instant now) {
        existing.setResolvedAt(now);
        return existing;
    }
}
