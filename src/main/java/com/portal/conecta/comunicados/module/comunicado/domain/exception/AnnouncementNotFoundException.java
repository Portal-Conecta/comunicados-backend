package com.portal.conecta.comunicados.module.comunicado.domain.exception;

public class AnnouncementNotFoundException extends RuntimeException {
    public AnnouncementNotFoundException() {
        super("Comunicado não encontrado.");
    }
    
    public AnnouncementNotFoundException(String message) {
        super(message);
    }
}
