package com.portal.conecta.comunicados.module.tag.domain.exception;

public class TagPermissionDeniedException extends RuntimeException {
    public TagPermissionDeniedException() {
        super("Usuário não tem permissão para associar / desassociar tags neste comunicado.");
    }
}
