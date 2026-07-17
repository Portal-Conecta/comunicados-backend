package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import java.time.Instant;
import java.util.List;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateAnnouncementRequest(

        @Size(max = 255)
        @Schema(description = "Título do comunicado.", example = "Reunião de alinhamento", maxLength = 255)
        String title,

        @Schema(description = "Conteúdo do comunicado em HTML sanitizado (allowlist TipTap). "
                + "O servidor deriva descriptionPlain automaticamente; não enviar plain no request.")
        String description,

        AnnouncementOrigin origin,

        @Schema(description = "Somente SCHEDULED → PUBLISHED (publicar agora) é aceito. "
                + "REMOVED e demais transições devem usar os endpoints dedicados.")
        AnnouncementStatus status,

        @Schema(description = "Ignorado no PUT — use PATCH /api/posts/{id}/pin e /unpin.")
        Boolean pinned,

        @Min(0)
        @Schema(description = "Ignorado no PUT — use PATCH /api/posts/{id}/pin.")
        Short pinnedOrder,

        @Schema(description = "Ignorado no PUT — use PATCH /api/posts/{id}/schedule.")
        Instant scheduledFor,

        @Valid
        List<CreateAnnouncementDestinationRequest> destinations

) {

    public static UpdateAnnouncementRequest fromEntity(Announcement entity) {
        return new UpdateAnnouncementRequest(
                entity.getTitle(),
                entity.getDescription(),
                entity.getOrigin(),
                entity.getStatus(),
                entity.isPinned(),
                entity.getPinnedOrder(),
                entity.getScheduledFor(),
                entity.getDestinations() == null
                        ? List.of()
                        : entity.getDestinations().stream()
                        .map(CreateAnnouncementDestinationRequest::fromEntity)
                        .toList()
        );
    }

}