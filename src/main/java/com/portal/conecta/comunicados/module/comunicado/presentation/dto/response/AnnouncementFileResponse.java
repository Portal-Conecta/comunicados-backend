package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import java.time.Instant;
import java.util.List;
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

    public static AnnouncementFileResponse fromEntity(AnnouncementFile entity) {
        return new AnnouncementFileResponse(
                entity.getId(),
                entity.getAnnouncement() != null ? entity.getAnnouncement().getId() : null,
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


    public static List<AnnouncementFileResponse> fromEntities(List<AnnouncementFile> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream().map(AnnouncementFileResponse::fromEntity).toList();
    }
}
