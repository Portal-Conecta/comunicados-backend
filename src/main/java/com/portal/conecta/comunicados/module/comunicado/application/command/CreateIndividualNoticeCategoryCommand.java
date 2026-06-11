package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.domain.model.IndividualNoticeCategory;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateIndividualNoticeCategoryRequest;

import java.time.Instant;
import java.util.UUID;

public record CreateIndividualNoticeCategoryCommand(

    CreateIndividualNoticeCategoryRequest data,
    UUID actorUserId

) {

    public static CreateIndividualNoticeCategoryCommand fromRequest(
            CreateIndividualNoticeCategoryRequest request,
            UUID actorUserId
    ) {
        return new CreateIndividualNoticeCategoryCommand(request, actorUserId);
    }

    public IndividualNoticeCategory toEntity(Instant now) {
        return IndividualNoticeCategory.builder()
                .name(data.name())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
