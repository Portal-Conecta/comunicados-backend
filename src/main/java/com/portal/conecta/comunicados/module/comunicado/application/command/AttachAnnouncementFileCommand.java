package com.portal.conecta.comunicados.module.comunicado.application.command;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileType;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;

public record AttachAnnouncementFileCommand(

        UUID announcementId,
        String originalName,
        String contentType,
        long sizeBytes,
        byte[] content,
        boolean isThumbnail,
        UUID uploadedByUserId

) {

    public static AttachAnnouncementFileCommand from(
            UUID announcementId,
            MultipartFile file,
            boolean isThumbnail,
            UUID uploadedByUserId
    ) {
        try {
            return new AttachAnnouncementFileCommand(
                    announcementId,
                    Objects.requireNonNullElse(file.getOriginalFilename(), "arquivo"),
                    file.getContentType(),
                    file.getSize(),
                    file.getBytes(),
                    isThumbnail,
                    uploadedByUserId
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o arquivo enviado.", e);
        }
    }

    public AnnouncementFile toEntity(
            Announcement announcement,
            String s3Key,
            String s3Bucket,
            AnnouncementFileType resolvedType,
            Instant now
    ) {
        return AnnouncementFile.builder()
                .announcement(announcement)
                .originalName(originalName)
                .s3Key(s3Key)
                .s3Bucket(s3Bucket)
                .contentType(contentType)
                .type(resolvedType)
                .fileStatus(AnnouncementFileStatus.READY)
                .sizeBytes(sizeBytes)
                .isThumbnail(isThumbnail)
                .uploadedByUserId(uploadedByUserId)
                .createdAt(now)
                .build();
    }
}
