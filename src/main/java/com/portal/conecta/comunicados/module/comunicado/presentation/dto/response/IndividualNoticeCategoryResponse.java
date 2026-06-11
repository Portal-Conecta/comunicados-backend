package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import com.portal.conecta.comunicados.module.comunicado.domain.model.IndividualNoticeCategory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IndividualNoticeCategoryResponse(

    UUID id,
    String name,
    Instant createdAt,
    Instant updatedAt

) {

    public static IndividualNoticeCategoryResponse fromEntity(IndividualNoticeCategory entity) {
        return new IndividualNoticeCategoryResponse(
                entity.getId(),
                entity.getName(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static List<IndividualNoticeCategoryResponse> fromEntities(List<IndividualNoticeCategory> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream().map(IndividualNoticeCategoryResponse::fromEntity).toList();
    }
}
