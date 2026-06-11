package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementIndividualNotice;
import com.portal.conecta.comunicados.module.comunicado.domain.model.IndividualNoticeCategory;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementIndividualNoticeRequest;

import java.util.UUID;

public record CreateAnnouncementIndividualNoticeCommand(

    CreateAnnouncementIndividualNoticeRequest data,
    UUID actorUserId

) {

    public static CreateAnnouncementIndividualNoticeCommand fromRequest(
            CreateAnnouncementIndividualNoticeRequest request,
            UUID actorUserId
    ) {
        return new CreateAnnouncementIndividualNoticeCommand(request, actorUserId);
    }

    public AnnouncementIndividualNotice toEntity(Announcement announcement, IndividualNoticeCategory category) {
        return AnnouncementIndividualNotice.builder()
                .announcement(announcement)
                .category(category)
                .build();
    }
}
