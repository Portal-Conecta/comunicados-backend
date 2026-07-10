package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.PinAnnouncementRequest;

import java.util.UUID;

public record PinAnnouncementCommand(

        UUID announcementId,
        UUID pinnedByUserId,
        Integer pinnedOrder
) {

    public static PinAnnouncementCommand from(PinAnnouncementRequest request, UUID pinnedByUserId, UUID announcementId) {
        return new PinAnnouncementCommand(announcementId, pinnedByUserId, request.pinnedOrder());
    }

}
