package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.dto;

import java.util.List;

public record HubUserSearchResponse(
        List<HubUserResponse> users
) {
    public HubUserSearchResponse {
        users = users == null ? List.of() : List.copyOf(users);
    }
}
