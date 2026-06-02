package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import java.util.UUID;

public record AnnouncementTagResponse(

    UUID announcementId,
    UUID tagId,
    String tagName

) {}