package com.portal.conecta.comunicados.module.comunicado.domain.port.support;

import java.util.Optional;
import java.util.UUID;

import com.portal.conecta.comunicados.shared.context.UserType;

public interface HubUserPort {

    Optional<UserType> findUserTypeById(UUID userId);
}
