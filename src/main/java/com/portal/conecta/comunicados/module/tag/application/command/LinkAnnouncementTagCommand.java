package com.portal.conecta.comunicados.module.tag.application.command;

import com.portal.conecta.comunicados.module.tag.presentation.dto.request.LinkAnnouncementTagRequest;

import java.util.List;
import java.util.UUID;

public record LinkAnnouncementTagCommand(
        UUID announcementId,
        List<UUID> tagIds,
        UUID requestingUserId
) {
    public static LinkAnnouncementTagCommand from(
            UUID announcementId,
            LinkAnnouncementTagRequest request,
            UUID requestingUserId
    ) {
        return new LinkAnnouncementTagCommand(announcementId, request.tagIds(), requestingUserId);
    }
}

