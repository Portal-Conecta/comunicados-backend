package com.portal.conecta.comunicados.module.comunicado.domain.exception;

public class AnnouncementNotScheduledException extends RuntimeException {
    public AnnouncementNotScheduledException() {
        super("Apenas comunicados com status SCHEDULED podem ser reagendados.");
    }
}