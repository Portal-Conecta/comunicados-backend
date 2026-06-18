package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import org.springframework.data.domain.Page;

import java.util.List;

public record ListAnnouncementHistoryResponse(

    List<AnnouncementHistoryResponse> items,
    int page,
    int size,
    long totalElements,
    long totalPages

) {

    public static ListAnnouncementHistoryResponse fromPage(Page<AnnouncementHistory> page) {
        return new ListAnnouncementHistoryResponse(
                AnnouncementHistoryResponse.fromEntities(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

}