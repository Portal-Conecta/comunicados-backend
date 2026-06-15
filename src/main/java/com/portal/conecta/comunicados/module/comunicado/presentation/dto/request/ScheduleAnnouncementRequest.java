package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Body de {@code POST /api/posts/schedule} (#108): cria e agenda o comunicado numa única
 * transação. {@code scheduledFor} precisa ser futuro (a validação de borda do body já garante
 * 400; a regra de negócio é reforçada no use case via AnnouncementMustBeInTheFutureException).
 */
public record ScheduleAnnouncementRequest(

    @NotBlank
    String title,

    @NotBlank
    String description,

    @NotNull
    AnnouncementOrigin origin,

    @NotNull
    @Future
    Instant scheduledFor,

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
