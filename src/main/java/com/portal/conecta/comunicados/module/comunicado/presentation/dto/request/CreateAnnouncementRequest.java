package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

public record CreateAnnouncementRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Título não pode exceder 255 caracteres")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Origin is required")
        AnnouncementOrigin origin,

        @NotNull(message = "Stats is required")
        AnnouncementStatus status,

        Boolean pinned,

        @Min(value = 0, message = "Ordem de fixação não pode ser negativa")
        Short pinnedOrder,

        Instant scheduledFor,

        @NotEmpty(message = "Destiny is required")
        @Valid
        List<CreateAnnouncementDestinationRequest> destinations

) {
}