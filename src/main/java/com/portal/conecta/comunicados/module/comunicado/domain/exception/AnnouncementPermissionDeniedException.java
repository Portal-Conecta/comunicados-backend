package com.portal.conecta.comunicados.module.comunicado.domain.exception;

public class AnnouncementPermissionDeniedException extends RuntimeException {
    public AnnouncementPermissionDeniedException() {
        super("User does not have permission to create an announcement.");
    }
}
