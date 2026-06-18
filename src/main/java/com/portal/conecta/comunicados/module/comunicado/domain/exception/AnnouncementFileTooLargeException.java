package com.portal.conecta.comunicados.module.comunicado.domain.exception;

public class AnnouncementFileTooLargeException extends RuntimeException {

    public AnnouncementFileTooLargeException() {
        super("Arquivo excede o tamanho máximo permitido.");
    }
}
