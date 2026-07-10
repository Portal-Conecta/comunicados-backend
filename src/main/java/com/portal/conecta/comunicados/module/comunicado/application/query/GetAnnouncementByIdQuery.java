package com.portal.conecta.comunicados.module.comunicado.application.query;

import java.util.UUID;

public record GetAnnouncementByIdQuery(

    UUID id,
    UUID viewerUserId

) {}