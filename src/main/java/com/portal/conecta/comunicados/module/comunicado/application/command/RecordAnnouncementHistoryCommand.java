package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementHistoryRequest;

public record RecordAnnouncementHistoryCommand(

    CreateAnnouncementHistoryRequest data

) {}