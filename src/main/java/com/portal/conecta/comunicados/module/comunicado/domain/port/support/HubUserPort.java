package com.portal.conecta.comunicados.module.comunicado.domain.port.support;

import com.portal.conecta.comunicados.module.comunicado.domain.model.hub.HubUser;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.UserType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HubUserPort {

    boolean existsById(UUID userId);

    Optional<HubUser> findById(UUID userId);

    Optional<UserType> findUserTypeById(UUID userId);

    List<UUID> findUserIdsByNameContaining(String term, RequestContext context);
}
