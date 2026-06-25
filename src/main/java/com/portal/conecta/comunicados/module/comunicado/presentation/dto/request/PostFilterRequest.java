package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springdoc.core.annotations.ParameterObject;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@ParameterObject
public record PostFilterRequest(

    @Schema(
            description = "Filtra por origem do comunicado",
            allowableValues = {"WEG", "SENAI", "BOTH"},
            example = "SENAI"
    )
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

    @Schema(
            description = "Filtra comunicados vinculados à tag informada",
            example = "66666666-6666-6666-6666-666666666666"
    )
    UUID tagId,

    @Schema(
            description = "Filtra por uma ou mais tags (semântica OR). "
                    + "Repita o parâmetro (`tagIds=uuid1&tagIds=uuid2`) ou separe por vírgula. "
                    + "Tag inexistente retorna lista vazia.",
            example = "[\"66666666-6666-6666-6666-666666666666\"]"
    )
    List<UUID> tagIds,

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
        return new PostFilterRequest(null, null, null, null, null, null, null, null, null, null);
    }

    public static PostFilterRequest withOrigin(AnnouncementOrigin origin) {
        return new PostFilterRequest(origin, null, null, null, null, null, null, null, null, null);
    }

    public static PostFilterRequest withPaging(int page, int size) {
        return new PostFilterRequest(null, null, null, null, null, null, null, null, page, size);
    }

    public static PostFilterRequest withSearch(String search) {
        return new PostFilterRequest(null, null, null, null, null, search, null, null, null, null);
    }

    public static PostFilterRequest withTagId(UUID tagId) {
        return new PostFilterRequest(null, null, null, null, null, null, tagId, null, null, null);
    }

    public static PostFilterRequest withTagIds(List<UUID> tagIds) {
        return new PostFilterRequest(null, null, null, null, null, null, null, tagIds, null, null);
    }

    public List<UUID> resolvedTagIds() {
        LinkedHashSet<UUID> ids = new LinkedHashSet<>();
        if (tagId != null) {
            ids.add(tagId);
        }
        if (tagIds != null) {
            ids.addAll(tagIds);
        }
        return List.copyOf(ids);
    }

}
