package com.portal.conecta.comunicados.module.tag.presentation.dto.request;

import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import jakarta.validation.constraints.Size;

public record UpdateTagRequest(

    @Size(max = 255)
    String name,

    Boolean active

) {

    public static UpdateTagRequest fromEntity(Tag entity) {
        return new UpdateTagRequest(entity.getName(), entity.isActive());
    }
}