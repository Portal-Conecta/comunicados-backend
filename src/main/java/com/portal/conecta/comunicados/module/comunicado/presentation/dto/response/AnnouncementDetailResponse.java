package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import java.util.List;

public record AnnouncementDetailResponse(

    AnnouncementResponse announcement,
    List<AnnouncementDestinationResponse> destinations,
    List<AnnouncementFileResponse> files,
    List<AnnouncementTagResponse> tags,
    List<AnnouncementMentionResponse> mentions

) {}