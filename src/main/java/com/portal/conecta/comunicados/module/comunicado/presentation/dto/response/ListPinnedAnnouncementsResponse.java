package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;

import java.util.List;

public record ListPinnedAnnouncementsResponse(

        List<AnnouncementSummaryResponse> items

) {

    public static ListPinnedAnnouncementsResponse fromEntities(List<Announcement> announcements) {
        return new ListPinnedAnnouncementsResponse(AnnouncementSummaryResponse.fromEntities(announcements));
    }
}
