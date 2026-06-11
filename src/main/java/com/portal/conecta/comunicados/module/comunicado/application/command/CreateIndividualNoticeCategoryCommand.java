package com.portal.conecta.comunicados.module.comunicado.application.command;

import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateIndividualNoticeCategoryRequest;

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
}
