package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementHistoryRequest;

public record RecordAnnouncementHistoryCommand(

    CreateAnnouncementHistoryRequest data

) {

    public static RecordAnnouncementHistoryCommand fromRequest(CreateAnnouncementHistoryRequest request) {
        return new RecordAnnouncementHistoryCommand(request);
    }

    public static RecordAnnouncementHistoryCommand fromEntity(AnnouncementHistory entity) {
        return new RecordAnnouncementHistoryCommand(CreateAnnouncementHistoryRequest.fromEntity(entity));
    }
}
