package com.portal.conecta.comunicados.module.tag.application.command;

import com.portal.conecta.comunicados.module.tag.presentation.dto.request.CreateTagRequest;

public record CreateTagCommand(

    CreateTagRequest data

) {

    public static CreateTagCommand fromRequest(CreateTagRequest request) {
        return new CreateTagCommand(request);
    }
}
