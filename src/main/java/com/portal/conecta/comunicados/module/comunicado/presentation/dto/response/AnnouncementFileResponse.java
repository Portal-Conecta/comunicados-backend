package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileType;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;

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

) {

    public static AnnouncementFileResponse from(AnnouncementFile entity) {
        return new AnnouncementFileResponse(
                entity.getId(),
                entity.getAnnouncement().getId(),
                entity.getOriginalName(),
                entity.getS3Key(),
                entity.getS3Bucket(),
                entity.getContentType(),
                entity.getType(),
                entity.getSizeBytes(),
                entity.isThumbnail(),
                entity.getUploadedByUserId(),
                entity.getCreatedAt()
        );
    }
}