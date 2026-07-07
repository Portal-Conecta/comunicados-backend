package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.dto;

import java.util.UUID;

public record HubCourseResponse(
        UUID id,
        String name
) {
}
