package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import java.util.List;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body de {@code POST /api/posts/publish} (#107): cria e publica o comunicado numa única
 * transação. O autor/publicador vem do contexto autenticado, não do body.
 */
public record PublishAnnouncementRequest(

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Título do comunicado.", example = "Reunião de alinhamento", maxLength = 255)
    String title,

    @NotBlank
    @Schema(description = "Conteúdo do comunicado em texto livre, sem limite de tamanho.")
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
