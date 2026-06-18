package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RescheduleAnnouncementRequest(

        @NotNull(message = "A data de agendamento é obrigatória.")
        @Future(message = "A data de agendamento deve ser futura.")
        Instant scheduledFor

) {}