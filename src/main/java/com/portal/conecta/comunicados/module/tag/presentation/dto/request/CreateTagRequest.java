package com.portal.conecta.comunicados.module.tag.presentation.dto.request;

import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTagRequest(

    @NotBlank
    @Size(max = 255)
    String name,

    @NotNull
    TagEntityType entityType,

    Boolean active

) {

    public static CreateTagRequest fromEntity(Tag entity) {
        return new CreateTagRequest(entity.getName(), entity.getEntityType(), entity.isActive());
    }
}