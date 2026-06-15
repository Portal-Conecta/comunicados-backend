package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springdoc.core.annotations.ParameterObject;

import java.time.Instant;
import java.util.UUID;

@ParameterObject
public record PostFilterRequest(

    AnnouncementOrigin origin,
    String filterType,
    UUID classId,
    Instant publishedFrom,
    Instant publishedTo,

    @Schema(
            description = "Termo de busca textual (título, descrição, nome de tag ou destinatário)",
            example = "retirada"
    )
    String search,

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

    public static PostFilterRequest defaults() {
        return new PostFilterRequest(null, null, null, null, null, null, null, null);
    }

    public static PostFilterRequest withOrigin(AnnouncementOrigin origin) {
        return new PostFilterRequest(origin, null, null, null, null, null, null, null);
    }

    public static PostFilterRequest withPaging(int page, int size) {
        return new PostFilterRequest(null, null, null, null, null, null, page, size);
    }

    public static PostFilterRequest withSearch(String search) {
        return new PostFilterRequest(null, null, null, null, null, search, null, null);
    }

}
