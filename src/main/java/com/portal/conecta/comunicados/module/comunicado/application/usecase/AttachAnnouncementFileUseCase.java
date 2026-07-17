package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.portal.conecta.comunicados.module.comunicado.application.command.AttachAnnouncementFileCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileType;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementFileContentTypeNotAllowedException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementFileLimitExceededException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementFileTooLargeException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementFileRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StoragePort;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StorageUploadResult;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.storage.StorageProperties;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;

@Service
public class AttachAnnouncementFileUseCase {

    private static final String S3_KEY_PREFIX = "comunicados/";

    private static final Map<String, String> CONTENT_TYPE_TO_EXT = Map.ofEntries(
            Map.entry("image/jpeg", "jpg"),
            Map.entry("image/png", "png"),
            Map.entry("image/gif", "gif"),
            Map.entry("image/webp", "webp"),
            Map.entry("application/pdf", "pdf"),
            Map.entry("video/mp4", "mp4"),
            Map.entry("video/webm", "webm")
    );

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementFileRepository fileRepository;
    private final RequestContextProvider contextProvider;
    private final AnnouncementPermissionValidator permissionValidator;
    private final StoragePort storagePort;
    private final StorageProperties storageProperties;
    private final int maxFilesPerAnnouncement;
    private final long maxFileSizeBytes;

    public AttachAnnouncementFileUseCase(
            AnnouncementRepository announcementRepository,
            AnnouncementFileRepository fileRepository,
            RequestContextProvider contextProvider,
            AnnouncementPermissionValidator permissionValidator,
            StoragePort storagePort,
            StorageProperties storageProperties,

            @Value("${storage.max-files-per-announcement:5}")
            int maxFilesPerAnnouncement,

            @Value("${storage.max-file-size-mb:10}")
            int maxFileSizeMb
    ) {
        this.announcementRepository = announcementRepository;
        this.fileRepository = fileRepository;
        this.contextProvider = contextProvider;
        this.permissionValidator = permissionValidator;
        this.storagePort = storagePort;
        this.storageProperties = storageProperties;
        this.maxFilesPerAnnouncement = maxFilesPerAnnouncement;
        this.maxFileSizeBytes = (long) maxFileSizeMb * 1024 * 1024;
    }

    @Transactional
    public AnnouncementFile execute(AttachAnnouncementFileCommand command) {
        RequestContext context = contextProvider.getRequestContext();

        Announcement announcement = announcementRepository
                .findByIdAndRemovedAtIsNull(command.announcementId())
                .orElseThrow(AnnouncementNotFoundException::new);

        if (!permissionValidator.canUpdate(context.userType(), context.userId(), announcement)) {
            throw new AnnouncementNotFoundException();
        }

        if (fileRepository.countByAnnouncementId(command.announcementId()) >= maxFilesPerAnnouncement) {
            throw new AnnouncementFileLimitExceededException();
        }

        AnnouncementFileType fileType = AnnouncementFileType.fromContentType(command.contentType())
                .orElseThrow(() -> new AnnouncementFileContentTypeNotAllowedException(command.contentType()));

        long maxBytes = resolveMaxBytes(command.contentType());
        if (command.sizeBytes() > maxBytes) {
            throw new AnnouncementFileTooLargeException();
        }

        if (command.isThumbnail()) {
            fileRepository.clearAllThumbnailsByAnnouncementId(command.announcementId());
        }

        // Mesmo padrão de chave do presign — Lambda processa raw → processed.
        UUID keyId = UUID.randomUUID();
        String ext = CONTENT_TYPE_TO_EXT.getOrDefault(command.contentType(), "bin");
        String s3Key = S3_KEY_PREFIX + command.uploadedByUserId() + "/raw/" + keyId + "." + ext;

        StorageUploadResult upload = storagePort.upload(s3Key, command.contentType(), command.content());
        registerStorageRollbackCompensation(upload.s3Key(), upload.s3Bucket());

        AnnouncementFileStatus status = upload.awaitsAsyncProcessing()
                ? AnnouncementFileStatus.PENDING
                : AnnouncementFileStatus.READY;
        // Em mock/local o arquivo já está disponível; em S3 aguarda processedS3Key via Lambda/reconcile.
        String processedS3Key = upload.awaitsAsyncProcessing() ? null : upload.s3Key();

        AnnouncementFile file = command.toEntity(
                announcement,
                upload.s3Key(),
                upload.s3Bucket(),
                fileType,
                status,
                processedS3Key,
                Instant.now()
        );
        return fileRepository.save(file);
    }

    private long resolveMaxBytes(String contentType) {
        return switch (contentType) {
            case "image/jpeg", "image/png", "image/gif", "image/webp" -> storageProperties.maxImageSizeBytes();
            case "application/pdf" -> storageProperties.maxDocumentSizeBytes();
            case "video/mp4", "video/webm" -> storageProperties.maxVideoSizeBytes();
            default -> storageProperties.maxFileSizeBytes();
        };
    }

    /**
     * Remove o objeto recém-enviado ao storage caso a transação seja revertida (#130).
     */
    private void registerStorageRollbackCompensation(String s3Key, String s3Bucket) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    storagePort.delete(s3Key, s3Bucket);
                }
            }
        });
    }
}
