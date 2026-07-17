package com.portal.conecta.comunicados.module.comunicado.domain.port.support;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.model.hub.HubClassInfo;
import com.portal.conecta.comunicados.module.comunicado.domain.model.hub.HubStudent;

public interface HubClassPort {

    boolean existsById(UUID classId);

    Optional<HubClassInfo> findClassById(UUID classId);

    /**
     * Alunos e representantes das turmas do usuário autenticado (JWT),
     * via {@code GET /me/classes/students} — sem fan-out por turma.
     */
    List<HubStudent> findMyClassStudents();

    UUID getClassIdForUser(UUID userId);
}
