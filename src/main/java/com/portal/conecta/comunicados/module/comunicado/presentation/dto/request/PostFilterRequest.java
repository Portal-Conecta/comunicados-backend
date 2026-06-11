package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;
import java.util.UUID;

public record PostFilterRequest(

    AnnouncementOrigin origin,
    String filterType,
    UUID classId,
    Instant publishedFrom,
    Instant publishedTo,

    @Min(0)
    Integer page,

    @Min(1)
    @Max(100)
    Integer size

) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    public PostFilterRequest {
        if (page == null) {
            page = DEFAULT_PAGE;
        }
        if (size == null || size == 0) {
            size = DEFAULT_SIZE;
        }
    }

}
