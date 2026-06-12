package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * Destino informado no mesmo request em que o comunicado nasce (#107 / #108). Diferente de
 * {@link CreateAnnouncementDestinationRequest}, não carrega {@code announcementId}, pois o
 * comunicado ainda não existe quando o destino é enviado.
 */
public record CreateAnnouncementDestinationInput(

    @NotNull
    AnnouncementDestinationType type,

    UUID referenceId

) {

    @JsonIgnore
    @AssertTrue(message = "referenceId é obrigatório quando type não é GENERAL")
    public boolean isReferenceIdValid() {
        return type == null
                || type == AnnouncementDestinationType.GENERAL
                || referenceId != null;
    }
}
