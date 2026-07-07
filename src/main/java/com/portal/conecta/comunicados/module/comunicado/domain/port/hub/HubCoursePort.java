package com.portal.conecta.comunicados.module.comunicado.domain.port.hub;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolve os cursos do usuário autenticado consultando o Hub (core).
 * O comunicados não gerencia turmas/cursos: ele lê o vínculo do usuário no Hub
 * para conseguir aplicar o escopo de visibilidade em destinos do tipo COURSE.
 */
public interface HubCoursePort {

    /**
     * @return IDs dos cursos do usuário autenticado. Lista vazia quando não há
     * vínculos ou quando o Hub está indisponível (degrada sem derrubar a leitura).
     */
    List<UUID> getCurrentUserCourseIds();

    Optional<String> findCourseNameById(UUID courseId);
}
