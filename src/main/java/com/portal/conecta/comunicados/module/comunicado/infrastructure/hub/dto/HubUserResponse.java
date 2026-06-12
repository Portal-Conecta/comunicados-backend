package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.dto;

import java.util.UUID;

public record HubUserResponse(
        UUID id,
        String name,
        String userType
) {
}
