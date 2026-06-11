package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.LinkAnnouncementTagRequest;

public record LinkAnnouncementTagCommand(

    LinkAnnouncementTagRequest data

) {

    public static LinkAnnouncementTagCommand fromRequest(LinkAnnouncementTagRequest request) {
        return new LinkAnnouncementTagCommand(request);
    }
}
