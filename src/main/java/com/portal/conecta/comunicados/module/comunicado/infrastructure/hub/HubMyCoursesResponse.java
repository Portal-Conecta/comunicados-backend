package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub;

import java.util.List;
import java.util.UUID;

/**
 * Subconjunto da resposta de GET /me/courses do Hub. Só interessa o id do curso;
 * os demais campos (name, code, classes) são ignorados na desserialização.
 */
public record HubMyCoursesResponse(List<HubCourse> courses) {

    public record HubCourse(UUID id) {}
}
