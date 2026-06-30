package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;

import io.swagger.v3.oas.annotations.media.Schema;

public record ListAnnouncementsResponse(

    @Schema(description = "Comunicados fixados visíveis ao perfil, ordenados por pinnedOrder ASC")
    List<AnnouncementSummaryResponse> pinned,

    @Schema(description = "Comunicados não fixados, paginados por publishedAt DESC")
    List<AnnouncementSummaryResponse> items,

    int page,
    int size,
    long totalElements,
    long totalPages

) {

    public static ListAnnouncementsResponse fromPinnedAndPage(
            List<Announcement> pinned,
            Page<Announcement> page
    ) {
        return new ListAnnouncementsResponse(
                AnnouncementSummaryResponse.fromEntities(pinned),
                page.getContent()
                        .stream()
                        .map(AnnouncementSummaryResponse::fromEntity)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
