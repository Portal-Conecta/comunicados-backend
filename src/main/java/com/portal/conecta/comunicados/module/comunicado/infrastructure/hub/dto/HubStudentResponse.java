package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.dto;

import java.util.UUID;

public record HubStudentResponse(
        UUID id,
        String name
) {
}
