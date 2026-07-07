package com.portal.conecta.comunicados.module.comunicado.domain.port.hub;

import java.util.List;
import java.util.UUID;

/**
 * Resolve os turnos das turmas do usuário autenticado via Hub.
 */
public interface HubShiftPort {

    /**
     * @return códigos de turno distintos das turmas informadas (ex.: FULL_AM_PM).
     */
    List<String> getShiftCodesForClasses(List<UUID> classIds);
}
