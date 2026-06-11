package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import java.util.List;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;

public record AnnouncementDetailResponse(

    AnnouncementResponse announcement,
    List<AnnouncementDestinationResponse> destinations,
    List<AnnouncementFileResponse> files,
    List<AnnouncementTagResponse> tags,
    List<AnnouncementMentionResponse> mentions

) {

    public static AnnouncementDetailResponse from(Announcement entity) {
        return new AnnouncementDetailResponse(
                AnnouncementResponse.from(entity),
                entity.getDestinations().stream().map(AnnouncementDestinationResponse::from).toList(),
                entity.getFiles().stream().map(AnnouncementFileResponse::from).toList(),
                entity.getTags().stream().map(AnnouncementTagResponse::from).toList(),
                entity.getMentions().stream().map(AnnouncementMentionResponse::from).toList()
        );
    }
}
