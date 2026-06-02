package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import java.util.UUID;

public record PublishAnnouncementRequest(

        UUID publishedByUserId
        
) {}