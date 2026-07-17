package com.portal.conecta.comunicados.module.tag.presentation.dto.response;

import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TagResponse(

    UUID id,
    String name,
    TagEntityType entityType,
    String hubEntityId,
    Boolean active,
    Instant createdAt

) {

    public static TagResponse fromEntity(Tag entity) {
        return new TagResponse(
                entity.getId(),
                entity.getName(),
                entity.getEntityType(),
                entity.getHubEntityId(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }

    public static List<TagResponse> fromEntities(List<Tag> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream().map(TagResponse::fromEntity).toList();
    }
}
