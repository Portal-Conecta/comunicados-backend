package com.portal.conecta.comunicados.module.tag.domain.exception;

import java.util.List;

import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;

public class InvalidTagCodeException extends RuntimeException {

    public InvalidTagCodeException(TagEntityType entityType, List<String> codes) {
        super("Não foi possível vincular tags " + entityType + " para os códigos: " + String.join(", ", codes));
    }
}
