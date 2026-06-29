package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portal.conecta.comunicados.module.comunicado.application.command.PresignUploadAnnouncementFileCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileType;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementFileContentTypeNotAllowedException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementFileLimitExceededException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementFileTooLargeException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementPermissionDeniedException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementFileRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.presign.PresignedUpload;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StoragePort;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.storage.StorageProperties;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.response.PresignUploadResponse;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PresignUploadAnnouncementFileUseCase {

    private static final String S3_KEY_PREFIX = "comunicados/";

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementFileRepository fileRepository;
    private final RequestContextProvider contextProvider;
    private final AnnouncementPermissionValidator permissionValidator;
    private final StoragePort storagePort;
    private final StorageProperties storageProperties;

    @Transactional
    public PresignUploadResponse execute(PresignUploadAnnouncementFileCommand command) {
        RequestContext context = contextProvider.getRequestContext();

        Announcement announcement = announcementRepository
                .findByIdAndRemovedAtIsNull(command.announcementId())
                .orElseThrow(AnnouncementNotFoundException::new);

        if (!permissionValidator.canUpdate(context.userType(), context.userId(), announcement)) {
            throw new AnnouncementPermissionDeniedException();
        }

        if (fileRepository.countByAnnouncementId(command.announcementId()) >= storageProperties.maxFilesPerAnnouncement()) {
            throw new AnnouncementFileLimitExceededException();
        }

        AnnouncementFileType fileType = AnnouncementFileType.fromContentType(command.contentType())
                .orElseThrow(() -> new AnnouncementFileContentTypeNotAllowedException(command.contentType()));

        long maxBytes = resolveMaxBytes(command.contentType());
        if (command.sizeBytes() > maxBytes) {
            throw new AnnouncementFileTooLargeException();
        }

        // Pré-gera o UUID e usa como ID da entidade E sufixo da chave S3.
        // O Lambda extrai o fileId da chave raw (comunicados/{fileId}) para montar
        // a chave de saída determinística e para o backend identificar o registro.
        UUID fileId = UUID.randomUUID();
        String s3Key = S3_KEY_PREFIX + fileId;

        PresignedUpload presignedUpload = storagePort.presignUpload(s3Key, command.contentType(), maxBytes);

        AnnouncementFile file = AnnouncementFile.builder()
                .id(fileId)
                .announcement(announcement)
                .originalName(command.originalName())
                .s3Key(s3Key)
                .s3Bucket(presignedUpload.bucket())
                .contentType(command.contentType())
                .type(fileType)
                .fileStatus(AnnouncementFileStatus.PENDING)
                .sizeBytes(command.sizeBytes())
                .isThumbnail(command.isThumbnail())
                .uploadedByUserId(command.uploadedByUserId())
                .createdAt(Instant.now())
                .build();

        AnnouncementFile saved = fileRepository.save(file);

        return new PresignUploadResponse(saved.getId(), presignedUpload.url(), presignedUpload.fields());
    }

    private long resolveMaxBytes(String contentType) {
        return switch (contentType) {
            case "image/jpeg", "image/png", "image/gif", "image/webp" -> storageProperties.maxImageSizeBytes();
            case "image/svg+xml", "application/pdf"                   -> storageProperties.maxDocumentSizeBytes();
            case "video/mp4", "video/webm"                            -> storageProperties.maxVideoSizeBytes();
            default                                                    -> storageProperties.maxFileSizeBytes();
        };
    }
}
