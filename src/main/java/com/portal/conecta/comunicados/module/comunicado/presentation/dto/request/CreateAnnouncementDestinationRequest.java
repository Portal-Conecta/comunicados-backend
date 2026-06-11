package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record CreateAnnouncementDestinationRequest(

    @NotNull
    UUID announcementId,

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

    public static CreateAnnouncementDestinationRequest fromEntity(AnnouncementDestination entity) {
        return new CreateAnnouncementDestinationRequest(
                entity.getAnnouncement() != null ? entity.getAnnouncement().getId() : null,
                entity.getType(),
                entity.getReferenceId()
        );
    }
}