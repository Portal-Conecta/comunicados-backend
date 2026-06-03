package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementIndividualNoticeRequest;

import java.util.UUID;

public record CreateAnnouncementIndividualNoticeCommand(

    CreateAnnouncementIndividualNoticeRequest data,
    UUID actorUserId

) {}