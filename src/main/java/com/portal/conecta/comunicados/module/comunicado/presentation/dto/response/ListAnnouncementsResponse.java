package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;

public record ListAnnouncementsResponse(

    List<AnnouncementSummaryResponse> items,
    int page,
    int size,
    long totalElements,
    long totalPages

) {

    public static ListAnnouncementsResponse fromPage(Page<Announcement> page) {
        return new ListAnnouncementsResponse(
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
