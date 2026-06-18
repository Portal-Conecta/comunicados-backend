package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portal.conecta.comunicados.module.comunicado.application.command.AttachAnnouncementFileCommand;
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
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StoragePort;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StorageUploadResult;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;

@Service
public class AttachAnnouncementFileUseCase {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementFileRepository fileRepository;
    private final RequestContextProvider contextProvider;
    private final AnnouncementPermissionValidator permissionValidator;
    private final StoragePort storagePort;
    private final int maxFilesPerAnnouncement;
    private final long maxFileSizeBytes;

    public AttachAnnouncementFileUseCase(
            AnnouncementRepository announcementRepository,
            AnnouncementFileRepository fileRepository,
            RequestContextProvider contextProvider,
            AnnouncementPermissionValidator permissionValidator,
            StoragePort storagePort,

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
            throw new AnnouncementPermissionDeniedException();
        }

        if (command.sizeBytes() > maxFileSizeBytes) {
            throw new AnnouncementFileTooLargeException();
        }

        AnnouncementFileType fileType = AnnouncementFileType.fromContentType(command.contentType())
                .orElseThrow(() -> new AnnouncementFileContentTypeNotAllowedException(command.contentType()));

        if (fileRepository.countByAnnouncementId(command.announcementId()) >= maxFilesPerAnnouncement) {
            throw new AnnouncementFileLimitExceededException();
        }

        if (command.isThumbnail()) {
            fileRepository.clearAllThumbnailsByAnnouncementId(command.announcementId());
        }

        StorageUploadResult upload = storagePort.upload(command.contentType(), command.content());

        AnnouncementFile file = command.toEntity(announcement, upload.s3Key(), upload.s3Bucket(), fileType, Instant.now());
        return fileRepository.save(file);
    }
}
