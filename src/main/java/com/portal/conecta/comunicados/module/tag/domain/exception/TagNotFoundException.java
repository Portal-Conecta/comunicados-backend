package com.portal.conecta.comunicados.module.tag.domain.exception;

import java.util.UUID;

public class TagNotFoundException extends RuntimeException {
    public TagNotFoundException(UUID tagId) {
        super("Tag não encontrada: "+ tagId);
    }
}
