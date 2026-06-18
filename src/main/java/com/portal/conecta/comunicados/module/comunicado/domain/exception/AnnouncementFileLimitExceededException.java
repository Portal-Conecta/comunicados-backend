package com.portal.conecta.comunicados.module.comunicado.domain.exception;

public class AnnouncementFileLimitExceededException extends RuntimeException {

    public AnnouncementFileLimitExceededException() {
        super("Limite de arquivos por comunicado atingido (máx. 5).");
    }
}
