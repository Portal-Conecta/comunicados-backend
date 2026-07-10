package com.portal.conecta.comunicados.module.comunicado.domain.port.support;

import java.util.UUID;

public interface HubRoomPort {

    boolean existsById(UUID roomId);

    boolean isUserLinkedToRoom(UUID userId, UUID roomId);
}
