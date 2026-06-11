package com.portal.conecta.comunicados.module.comunicado.domain.exception;

public class AnnouncementNotFoundException extends RuntimeException {
    public AnnouncementNotFoundException(String message) {
        super(message);
    }

    public AnnouncementNotFoundException() {
        super("Comunicado não encontrado");
    }
}
