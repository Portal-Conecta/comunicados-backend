package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.dto;

import java.util.UUID;

/**
 * Espelha {@code ClassMemberResponse} do Hub ({@code GET /me/classes/students}).
 * Campos extras além de id/name são opcionais para tolerar evolução do contrato.
 */
public record HubClassMemberResponse(
        UUID id,
        String name,
        String classRole,
        Boolean active,
        String accountStatus
) {
}
