package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import java.util.List;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Body de {@code POST /api/posts/publish} (#107): cria e publica o comunicado numa única
 * transação. O autor/publicador vem do contexto autenticado, não do body.
 */
public record PublishAnnouncementRequest(

    @NotBlank
    String title,

    @NotBlank
    String description,

    @NotNull
    AnnouncementOrigin origin,

    @NotEmpty
    @Valid
    List<CreateAnnouncementDestinationInput> destinations,

    Boolean pinned,

    List<UUID> tagIds

) {

    public boolean isPinned() {
        return Boolean.TRUE.equals(pinned);
    }
}
