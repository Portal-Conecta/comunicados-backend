package com.portal.conecta.comunicados.module.comunicado.domain.exception;

public class AnnouncementPermissionDeniedException extends RuntimeException {
    public AnnouncementPermissionDeniedException() {
        super("Usuário não tem permissão para realizar essa ação neste comunicado.");
    }

    public AnnouncementPermissionDeniedException(String message) {
        super(message);
    }
}
