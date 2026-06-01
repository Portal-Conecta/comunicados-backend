package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.ScheduleAnnouncementRequest;

import java.util.UUID;

public record ScheduleAnnouncementCommand(

        UUID id,
        ScheduleAnnouncementRequest data

) {}