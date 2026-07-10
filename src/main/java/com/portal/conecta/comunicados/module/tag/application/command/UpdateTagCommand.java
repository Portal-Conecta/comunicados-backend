package com.portal.conecta.comunicados.module.tag.application.command;

import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.presentation.dto.request.UpdateTagRequest;

import java.util.UUID;

public record UpdateTagCommand(

    UUID id,
    UpdateTagRequest data

) {

    public static UpdateTagCommand fromRequest(UUID id, UpdateTagRequest request) {
        return new UpdateTagCommand(id, request);
    }

    public static UpdateTagCommand fromEntity(Tag entity) {
        return new UpdateTagCommand(
                entity.getId(),
                new UpdateTagRequest(entity.getName(), entity.isActive())
        );
    }

    public Tag toEntity(Tag existing) {
        if (data.name() != null) {
            existing.setName(data.name());
        }
        if (data.active() != null) {
            existing.setActive(data.active());
        }
        return existing;
    }
}
