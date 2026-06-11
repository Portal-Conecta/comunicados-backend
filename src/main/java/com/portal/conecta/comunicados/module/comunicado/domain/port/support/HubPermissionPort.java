package com.portal.conecta.comunicados.module.comunicado.domain.port.support;

import java.util.List;
import java.util.UUID;

import com.portal.conecta.comunicados.shared.context.UserType;

public interface HubPermissionPort {

    List<UUID> getAccessibleClassIds(UUID userId, UserType userType);
}
