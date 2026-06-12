package com.portal.conecta.comunicados.module.tag.application.dto;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import java.util.UUID;

public record TagLinkDestinationCommand(
        AnnouncementDestinationType type,
        UUID referenceId
) {}
