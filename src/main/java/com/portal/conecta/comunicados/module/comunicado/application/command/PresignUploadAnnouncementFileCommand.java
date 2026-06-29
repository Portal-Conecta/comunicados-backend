package com.portal.conecta.comunicados.module.comunicado.application.command;

import java.util.UUID;

public record PresignUploadAnnouncementFileCommand(

        UUID announcementId,
        String contentType,
        long sizeBytes,
        String originalName,
        boolean isThumbnail,
        UUID uploadedByUserId

) {}
