package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileType;

public record AnnouncementFileResponse(

    UUID id,
    UUID announcementId,
    String originalName,
    String s3Key,
    String s3Bucket,
    String contentType,
    AnnouncementFileType type,
    Long sizeBytes,
    Boolean isThumbnail,
    UUID uploadedByUserId,
    Instant createdAt

) {}