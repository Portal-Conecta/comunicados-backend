package com.portal.conecta.comunicados.module.tag.presentation.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record LinkAnnouncementTagRequest(

        @NotEmpty(message = "A lista de tags não pode ser vazia.")
        List<UUID> tagIds
) {
}
