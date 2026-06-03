package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

public record IndividualNoticeCategoryResponse(

    UUID id,
    String name,
    Instant createdAt,
    Instant updatedAt

) {}