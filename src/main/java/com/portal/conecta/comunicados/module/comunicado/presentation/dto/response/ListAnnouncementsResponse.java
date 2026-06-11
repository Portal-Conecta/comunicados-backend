package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import org.springframework.data.domain.Page;

import java.util.List;

public record ListAnnouncementsResponse(

    List<AnnouncementSummaryResponse> items,
    int page,
    int size,
    long totalElements,
    long totalPages

) {

    public static ListAnnouncementsResponse from(Page<Announcement> page) {
        return new ListAnnouncementsResponse(
                page.getContent()
                        .stream()
                        .map(AnnouncementSummaryResponse::from)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
