package com.portal.conecta.comunicados.module.comunicado.application.command;

import java.time.Instant;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementFileRequest;

public record AttachAnnouncementFileCommand(

    CreateAnnouncementFileRequest data,
    UUID uploadedByUserId

) {

    public static AttachAnnouncementFileCommand fromRequest(
            CreateAnnouncementFileRequest request,
            UUID uploadedByUserId
    ) {
        return new AttachAnnouncementFileCommand(request, uploadedByUserId);
    }

    public AnnouncementFile toEntity(Announcement announcement, Instant now) {
        return AnnouncementFile.builder()
                .announcement(announcement)
                .originalName(data.originalName())
                .s3Key(data.s3Key())
                .s3Bucket(data.s3Bucket())
                .contentType(data.contentType())
                .type(data.type())
                .sizeBytes(data.sizeBytes())
                .isThumbnail(data.isThumbnail() != null && data.isThumbnail())
                .uploadedByUserId(uploadedByUserId)
                .createdAt(now)
                .build();
    }
}
