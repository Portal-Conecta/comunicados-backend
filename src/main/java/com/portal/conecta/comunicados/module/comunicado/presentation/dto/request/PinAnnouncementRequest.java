package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import jakarta.validation.constraints.Positive;

public record PinAnnouncementRequest(
        @Positive(message = "A ordem de fixação dever ser um número positivo")
        Integer pinnedOrder
) {}
