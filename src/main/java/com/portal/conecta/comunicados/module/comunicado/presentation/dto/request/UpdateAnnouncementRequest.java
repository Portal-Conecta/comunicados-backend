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

        @Schema(description = "Conteúdo do comunicado em texto livre, sem limite de tamanho.")
        String description,

        AnnouncementOrigin origin,

        AnnouncementStatus status,

        Boolean pinned,

        @Min(0)
        Short pinnedOrder,

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