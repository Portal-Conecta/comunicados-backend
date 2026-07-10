package com.portal.conecta.comunicados.module.tag.application.command;

import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.presentation.dto.request.CreateTagRequest;

import java.time.Instant;

public record CreateTagCommand(

    CreateTagRequest data

) {

    public static CreateTagCommand fromRequest(CreateTagRequest request) {
        return new CreateTagCommand(request);
    }

    public Tag toEntity(Instant now) {
        return Tag.builder()
                .name(data.name())
                .entityType(data.entityType())
                .active(data.active() == null || data.active())
                .createdAt(now)
                .build();
    }
}
