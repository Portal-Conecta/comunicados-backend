package com.portal.conecta.comunicados.module.comunicado.domain.exception;

public class AnnouncementFileContentTypeNotAllowedException extends RuntimeException {

    public AnnouncementFileContentTypeNotAllowedException(String contentType) {
        super("Tipo de arquivo não permitido: %s.".formatted(contentType));
    }
}
