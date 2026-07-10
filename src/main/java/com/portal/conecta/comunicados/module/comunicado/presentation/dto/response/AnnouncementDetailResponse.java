package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import java.util.List;

import com.portal.conecta.comunicados.module.comunicado.application.dto.AnnouncementFileView;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;

public record AnnouncementDetailResponse(

    AnnouncementResponse announcement,
    List<AnnouncementDestinationResponse> destinations,
    List<AnnouncementFileResponse> files,
    List<AnnouncementTagResponse> tags,
    List<AnnouncementMentionResponse> mentions

) {

    public static AnnouncementDetailResponse fromEntity(Announcement entity) {
        return fromEntity(entity, List.of());
    }

    public static AnnouncementDetailResponse fromEntity(
            Announcement entity,
            List<AnnouncementFileView> fileViews
    ) {
        List<AnnouncementFileResponse> files = (fileViews != null && !fileViews.isEmpty())
                ? AnnouncementFileResponse.fromViews(fileViews)
                : AnnouncementFileResponse.fromEntities(entity.getFiles());

        return new AnnouncementDetailResponse(
                AnnouncementResponse.fromEntity(entity),
                AnnouncementDestinationResponse.fromEntities(entity.getDestinations()),
                files,
                AnnouncementTagResponse.fromEntities(entity.getTags()),
                AnnouncementMentionResponse.fromEntities(entity.getMentions())
        );
    }
}
