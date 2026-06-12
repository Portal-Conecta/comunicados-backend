package com.portal.conecta.comunicados.module.tag.presentation.dto.response;

import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;

import java.time.Instant;
import java.util.UUID;

public record TagResponse(

    UUID id,
    String name,
    TagEntityType entityType,
    Boolean active,
    Instant createdAt

) {
    public static TagResponse from(Tag tag) {
        return new TagResponse(
                tag.getId(),
                tag.getName(),
                tag.getEntityType(),
                tag.isActive(),
                tag.getCreatedAt()
        );
    }
}