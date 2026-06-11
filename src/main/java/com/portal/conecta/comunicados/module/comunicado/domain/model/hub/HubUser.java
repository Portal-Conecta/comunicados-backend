package com.portal.conecta.comunicados.module.comunicado.domain.model.hub;

import java.util.UUID;

import com.portal.conecta.comunicados.shared.context.UserType;

public record HubUser(
        UUID id,
        String name,
        UserType userType
) {
}
