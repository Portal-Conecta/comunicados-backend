package com.portal.conecta.comunicados.module.comunicado.domain.exception;

public class AnnouncementMustBeInTheFutureException extends RuntimeException {
    public AnnouncementMustBeInTheFutureException() {
        super("A data de agendamento não pode ser no passado!");
    }
}
