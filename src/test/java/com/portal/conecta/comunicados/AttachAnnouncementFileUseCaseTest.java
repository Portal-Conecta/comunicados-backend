package com.portal.conecta.comunicados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.portal.conecta.comunicados.module.comunicado.application.command.AttachAnnouncementFileCommand;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.AttachAnnouncementFileUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileType;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
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
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubClassPort;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import com.portal.conecta.comunicados.shared.context.UserType;

@ExtendWith(MockitoExtension.class)
class AttachAnnouncementFileUseCaseTest {

    @Mock private AnnouncementRepository announcementRepository;
    @Mock private AnnouncementFileRepository fileRepository;
    @Mock private RequestContextProvider contextProvider;
    @Mock private StoragePort storagePort;

    @Spy
    private AnnouncementPermissionValidator permissionValidator =
            new AnnouncementPermissionValidator(org.mockito.Mockito.mock(HubClassPort.class));

    private AttachAnnouncementFileUseCase useCase;

    private UUID announcementId;
    private UUID userId;
    private Announcement announcement;
    private AttachAnnouncementFileCommand command;

    @BeforeEach
    void setUp() {
        useCase = new AttachAnnouncementFileUseCase(
                announcementRepository, fileRepository, contextProvider, permissionValidator, storagePort, 5, 10
        );

        announcementId = UUID.randomUUID();
        userId = UUID.randomUUID();

        announcement = Announcement.builder()
                .id(announcementId)
                .title("Comunicado")
                .description("Descrição")
                .origin(AnnouncementOrigin.SENAI)
                .status(AnnouncementStatus.PUBLISHED)
                .pinned(false)
                .createdByUserId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        command = new AttachAnnouncementFileCommand(
                announcementId, "foto.jpg", "image/jpeg", 1024L, new byte[]{1}, false, userId
        );
    }

    @Test
    void shouldUploadAndSaveFileSuccessfully() {
        mockContext(UserType.SENAI, userId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(fileRepository.countByAnnouncementId(announcementId)).thenReturn(0L);
        when(storagePort.upload(anyString(), any())).thenReturn(new StorageUploadResult("key-123", "bucket"));
        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AnnouncementFile saved = useCase.execute(command);

        assertThat(saved.getOriginalName()).isEqualTo("foto.jpg");
        assertThat(saved.getType()).isEqualTo(AnnouncementFileType.IMAGE);
        verify(storagePort).upload("image/jpeg", command.content());
        verify(fileRepository).save(any(AnnouncementFile.class));
    }

    @Test
    void shouldClearExistingThumbnailWhenNewFileIsMarkedAsThumbnail() {
        AttachAnnouncementFileCommand thumbnailCommand = new AttachAnnouncementFileCommand(
                announcementId, "capa.jpg", "image/jpeg", 1024L, new byte[]{1}, true, userId
        );

        mockContext(UserType.SENAI, userId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(fileRepository.countByAnnouncementId(announcementId)).thenReturn(0L);
        when(storagePort.upload(anyString(), any())).thenReturn(new StorageUploadResult("key-123", "bucket"));
        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(thumbnailCommand);

        verify(fileRepository).clearAllThumbnailsByAnnouncementId(announcementId);
    }

    @Test
    void shouldNotClearThumbnailWhenFileIsNotMarkedAsThumbnail() {
        mockContext(UserType.SENAI, userId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(fileRepository.countByAnnouncementId(announcementId)).thenReturn(0L);
        when(storagePort.upload(anyString(), any())).thenReturn(new StorageUploadResult("key-123", "bucket"));
        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(command);

        verify(fileRepository, never()).clearAllThumbnailsByAnnouncementId(any());
    }

    @Test
    void shouldThrowNotFoundWhenAnnouncementDoesNotExist() {
        mockContext(UserType.SENAI, userId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(AnnouncementNotFoundException.class);

        verify(storagePort, never()).upload(anyString(), any());
    }

    @Test
    void shouldThrowPermissionDeniedWhenStudentUploads() {
        mockContext(UserType.STUDENT, userId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(AnnouncementPermissionDeniedException.class);

        verify(storagePort, never()).upload(anyString(), any());
    }

    @Test
    void shouldThrowPermissionDeniedWhenTeacherIsNotOwner() {
        UUID otherUserId = UUID.randomUUID();
        mockContext(UserType.TEACHER, otherUserId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(AnnouncementPermissionDeniedException.class);
    }

    @Test
    void shouldThrowFileTooLargeWhenSizeExceedsLimit() {
        long elevenMb = 11L * 1024 * 1024;
        AttachAnnouncementFileCommand oversizedCommand = new AttachAnnouncementFileCommand(
                announcementId, "big.jpg", "image/jpeg", elevenMb, new byte[]{}, false, userId
        );

        mockContext(UserType.SENAI, userId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));

        assertThatThrownBy(() -> useCase.execute(oversizedCommand))
                .isInstanceOf(AnnouncementFileTooLargeException.class);
    }

    @Test
    void shouldThrowContentTypeNotAllowedWhenTypeIsInvalid() {
        AttachAnnouncementFileCommand invalidTypeCommand = new AttachAnnouncementFileCommand(
                announcementId, "virus.exe", "application/octet-stream", 512L, new byte[]{}, false, userId
        );

        mockContext(UserType.SENAI, userId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));

        assertThatThrownBy(() -> useCase.execute(invalidTypeCommand))
                .isInstanceOf(AnnouncementFileContentTypeNotAllowedException.class);
    }

    @Test
    void shouldThrowLimitExceededWhenFiveFilesAlreadyExist() {
        mockContext(UserType.SENAI, userId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(fileRepository.countByAnnouncementId(announcementId)).thenReturn(5L);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(AnnouncementFileLimitExceededException.class);

        verify(storagePort, never()).upload(anyString(), any());
    }

    @Test
    void shouldPrioritizeFileLimitOverSizeWhenBothViolated() {
        long elevenMb = 11L * 1024 * 1024;
        AttachAnnouncementFileCommand oversizedCommand = new AttachAnnouncementFileCommand(
                announcementId, "big.jpg", "image/jpeg", elevenMb, new byte[]{}, false, userId
        );

        mockContext(UserType.SENAI, userId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(fileRepository.countByAnnouncementId(announcementId)).thenReturn(5L);

        assertThatThrownBy(() -> useCase.execute(oversizedCommand))
                .isInstanceOf(AnnouncementFileLimitExceededException.class);

        verify(storagePort, never()).upload(anyString(), any());
    }

    @Test
    void shouldDeleteUploadedFileFromStorageWhenTransactionRollsBack() {
        mockContext(UserType.SENAI, userId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(fileRepository.countByAnnouncementId(announcementId)).thenReturn(0L);
        when(storagePort.upload(anyString(), any())).thenReturn(new StorageUploadResult("key-123", "bucket"));
        when(fileRepository.save(any())).thenThrow(new RuntimeException("falha ao persistir"));

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(RuntimeException.class);

            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(storagePort).delete("key-123", "bucket");
    }

    private void mockContext(UserType userType, UUID id) {
        when(contextProvider.getRequestContext()).thenReturn(new RequestContext(id, userType, List.of()));
    }
}
