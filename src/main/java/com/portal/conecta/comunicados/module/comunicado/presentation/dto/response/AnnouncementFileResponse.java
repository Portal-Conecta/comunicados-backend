package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.application.dto.AnnouncementFileView;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileType;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;

import io.swagger.v3.oas.annotations.media.Schema;

public record AnnouncementFileResponse(

    UUID id,
    UUID announcementId,
    String originalName,
    @Schema(description = "URL pré-assinada para download/exibição quando o arquivo está READY.")
    String displayUrl,
    String contentType,
    AnnouncementFileType type,
    AnnouncementFileStatus fileStatus,
    Long sizeBytes,
    Boolean isThumbnail,
    UUID uploadedByUserId,
    Instant createdAt

) {

    public static AnnouncementFileResponse fromView(AnnouncementFileView view) {
        AnnouncementFile entity = view.file();
        return new AnnouncementFileResponse(
                entity.getId(),
                entity.getAnnouncement() != null ? entity.getAnnouncement().getId() : null,
                entity.getOriginalName(),
                view.displayUrl(),
                entity.getContentType(),
                entity.getType(),
                entity.getFileStatus(),
                entity.getSizeBytes(),
                entity.isThumbnail(),
                entity.getUploadedByUserId(),
                entity.getCreatedAt()
        );
    }

    public static AnnouncementFileResponse fromEntity(AnnouncementFile entity) {
        return new AnnouncementFileResponse(
                entity.getId(),
                entity.getAnnouncement() != null ? entity.getAnnouncement().getId() : null,
                entity.getOriginalName(),
                null,
                entity.getContentType(),
                entity.getType(),
                entity.getFileStatus(),
                entity.getSizeBytes(),
                entity.isThumbnail(),
                entity.getUploadedByUserId(),
                entity.getCreatedAt()
        );
    }

    public static List<AnnouncementFileResponse> fromEntities(List<AnnouncementFile> entities) {
        if (entities == null || entities.isEmpty()) return List.of();
        return entities.stream().map(AnnouncementFileResponse::fromEntity).toList();
    }

    public static List<AnnouncementFileResponse> fromViews(List<AnnouncementFileView> views) {
        if (views == null || views.isEmpty()) return List.of();
        return views.stream().map(AnnouncementFileResponse::fromView).toList();
    }
}
