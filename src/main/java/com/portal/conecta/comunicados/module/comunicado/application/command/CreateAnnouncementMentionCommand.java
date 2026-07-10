package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementMention;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementMentionRequest;

public record CreateAnnouncementMentionCommand(

    CreateAnnouncementMentionRequest data

) {

    public static CreateAnnouncementMentionCommand fromRequest(CreateAnnouncementMentionRequest request) {
        return new CreateAnnouncementMentionCommand(request);
    }

    public AnnouncementMention toEntity(Announcement announcement) {
        return AnnouncementMention.builder()
                .announcement(announcement)
                .userId(data.userId())
                .build();
    }
}
