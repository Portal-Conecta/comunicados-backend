package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.ResolveAnnouncementIndividualNoticeRequest;

import java.util.UUID;

public record ResolveAnnouncementIndividualNoticeCommand(

    UUID id,
    ResolveAnnouncementIndividualNoticeRequest data,
    UUID actorUserId

) {}