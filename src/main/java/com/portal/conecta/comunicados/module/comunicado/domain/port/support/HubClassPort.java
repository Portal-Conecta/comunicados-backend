package com.portal.conecta.comunicados.module.comunicado.domain.port.support;

import java.util.List;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.model.hub.HubStudent;

public interface HubClassPort {

    boolean existsById(UUID classId);

    List<HubStudent> findStudentsByClassId(UUID classId);

    UUID getClassIdForUser(UUID userId);
}
