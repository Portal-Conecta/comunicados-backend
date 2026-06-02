package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ScheduleAnnouncementRequest(

    @NotNull
    @Future
    Instant scheduledFor

) {}