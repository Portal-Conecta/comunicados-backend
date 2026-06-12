package com.portal.conecta.comunicados.module.comunicado.domain.exception;

public class AnnouncementPermissionDeniedException extends RuntimeException {
    public AnnouncementPermissionDeniedException() {
        super("Usuário não tem permissão para criar um comunicado.");
    }
}
