package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

public record RescheduleAnnouncementRequest(

    @NotNull(message = "A data de agendamento é obrigatória.")
    @Future(message = "A data de agendamento deve ser fatura.")
    Instant scheduledFor
    
){}