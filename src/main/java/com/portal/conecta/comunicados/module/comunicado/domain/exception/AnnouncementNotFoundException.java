package com.portal.conecta.comunicados.module.comunicado.domain.exception;

public class AnnouncementNotFoundException extends RuntimeException {
    public AnnouncementNotFoundException() {
        super("Announcement not found.");
    }
}
