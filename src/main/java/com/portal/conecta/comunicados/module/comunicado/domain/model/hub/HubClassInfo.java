package com.portal.conecta.comunicados.module.comunicado.domain.model.hub;

import java.util.UUID;

public record HubClassInfo(
        UUID id,
        String name,
        String shiftCode
) {
}
