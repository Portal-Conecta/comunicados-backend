package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementMentionRequest;

public record CreateAnnouncementMentionCommand(

    CreateAnnouncementMentionRequest data

) {

    public static CreateAnnouncementMentionCommand fromRequest(CreateAnnouncementMentionRequest request) {
        return new CreateAnnouncementMentionCommand(request);
    }
}
