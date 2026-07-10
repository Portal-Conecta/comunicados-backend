package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementTag;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.LinkAnnouncementTagRequest;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;

public record LinkAnnouncementTagCommand(

    LinkAnnouncementTagRequest data

) {

    public static LinkAnnouncementTagCommand fromRequest(LinkAnnouncementTagRequest request) {
        return new LinkAnnouncementTagCommand(request);
    }

    public AnnouncementTag toEntity(Announcement announcement, Tag tag) {
        return AnnouncementTag.builder()
                .announcement(announcement)
                .tag(tag)
                .build();
    }
}
