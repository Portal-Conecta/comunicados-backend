package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.dto;

import java.util.UUID;

public record HubClassResponse(
        UUID id,
        String name,
        String shift
) {
}
