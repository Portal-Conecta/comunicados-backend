package com.portal.conecta.comunicados.module.comunicado.domain.exception;

public class AnnouncementFileNotFoundException extends RuntimeException {

    public AnnouncementFileNotFoundException() {
        super("Arquivo não encontrado.");
    }
}
