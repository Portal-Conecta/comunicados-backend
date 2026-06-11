package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;

import java.util.List;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;

public record AnnouncementDetailResponse(

    AnnouncementResponse announcement,
    List<AnnouncementDestinationResponse> destinations,
    List<AnnouncementFileResponse> files,
    List<AnnouncementTagResponse> tags,
    List<AnnouncementMentionResponse> mentions

) {

    public static AnnouncementDetailResponse fromEntity(Announcement entity) {
        return new AnnouncementDetailResponse(
                AnnouncementResponse.fromEntity(entity),
                AnnouncementDestinationResponse.fromEntities(entity.getDestinations()),
                AnnouncementFileResponse.fromEntities(entity.getFiles()),
                AnnouncementTagResponse.fromEntities(entity.getTags()),
                AnnouncementMentionResponse.fromEntities(entity.getMentions())
        );
    }
}
