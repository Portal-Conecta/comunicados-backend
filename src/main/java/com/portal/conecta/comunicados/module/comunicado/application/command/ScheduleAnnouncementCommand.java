package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.ScheduleAnnouncementRequest;

import java.util.UUID;

public record ScheduleAnnouncementCommand(

    UUID id,
    ScheduleAnnouncementRequest data

) {

    public static ScheduleAnnouncementCommand fromRequest(UUID id, ScheduleAnnouncementRequest request) {
        return new ScheduleAnnouncementCommand(id, request);
    }
}
