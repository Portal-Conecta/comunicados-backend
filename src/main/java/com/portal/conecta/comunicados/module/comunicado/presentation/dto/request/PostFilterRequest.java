package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

public record PostFilterRequest(

    AnnouncementOrigin origin,
    String filterType,
    UUID classId,

    @Min(0)
    int page,

    @Min(1)
    @Max(100)
    int size

) {

    public PostFilterRequest {
        if (size == 0) {
            size = 20;
        }
    }

}