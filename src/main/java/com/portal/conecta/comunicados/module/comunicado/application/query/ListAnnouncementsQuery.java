package com.portal.conecta.comunicados.module.comunicado.application.query;

import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.PostFilterRequest;

import java.util.UUID;

public record ListAnnouncementsQuery(

    PostFilterRequest filter,
    UUID viewerUserId

) {}