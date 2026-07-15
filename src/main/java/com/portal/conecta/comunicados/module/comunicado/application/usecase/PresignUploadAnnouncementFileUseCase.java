package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import java.time.Instant;
import java.util.Map;
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

    private static final Map<String, String> CONTENT_TYPE_TO_EXT = Map.ofEntries(
            Map.entry("image/jpeg",       "jpg"),
            Map.entry("image/png",        "png"),
            Map.entry("image/gif",        "gif"),
            Map.entry("image/webp",       "webp"),
            Map.entry("application/pdf",  "pdf"),
            Map.entry("video/mp4",        "mp4"),
            Map.entry("video/webm",       "webm")
    );

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
            throw new AnnouncementNotFoundException();
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

        // keyId é o UUID da CHAVE S3, não o ID da entidade. NÃO atribua o ID manualmente aqui:
        // com o ID já preenchido, o save() do Spring Data vira merge()→UPDATE numa linha
        // inexistente → StaleObjectStateException (HTTP 500). O PK é gerado pelo JPA no save,
        // como na flow multipart. A reconciliação (#172/#174) e a Lambda correlacionam arquivo
        // e registro pela s3Key (transformação de string), não pelo PK — por isso não coincidem.
        // Chave esperada pelo Lambda: {domain}/{ownerId}/raw/{keyId}.{ext}; processada: .../processed/{keyId}.
        UUID keyId = UUID.randomUUID();
        String ext = CONTENT_TYPE_TO_EXT.getOrDefault(command.contentType(), "bin");
        String s3Key = S3_KEY_PREFIX + command.uploadedByUserId() + "/raw/" + keyId + "." + ext;

        // contentLength = sizeBytes declarado: o PUT assinada exige Content-Length exato,
        // impedindo upload maior que o validado no servidor.
        PresignedUpload presignedUpload = storagePort.presignUpload(
                s3Key, command.contentType(), command.sizeBytes());

        AnnouncementFile file = AnnouncementFile.builder()
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
            case "application/pdf"                                    -> storageProperties.maxDocumentSizeBytes();
            case "video/mp4", "video/webm"                            -> storageProperties.maxVideoSizeBytes();
            default                                                    -> storageProperties.maxFileSizeBytes();
        };
    }
}
