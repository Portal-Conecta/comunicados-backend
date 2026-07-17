package com.portal.conecta.comunicados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.portal.conecta.comunicados.module.comunicado.application.command.AttachAnnouncementFileCommand;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.AttachAnnouncementFileUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileType;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
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
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubClassPort;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.storage.StorageProperties;
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
        StorageProperties storageProperties = new StorageProperties(
                true, 5, 10, 10, 20, 200, "comunicados-raw", "comunicados-processed", "us-east-1");
        useCase = new AttachAnnouncementFileUseCase(
                announcementRepository,
                fileRepository,
                contextProvider,
                permissionValidator,
                storagePort,
                storageProperties,
                5,
                10
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
    void shouldUploadAndSaveFileAsPendingWhenStorageIsAsync() {
        mockContext(UserType.SENAI, userId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(fileRepository.countByAnnouncementId(announcementId)).thenReturn(0L);
        when(storagePort.upload(anyString(), anyString(), any()))
                .thenAnswer(inv -> StorageUploadResult.async(inv.getArgument(0), "bucket"));
        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AnnouncementFile saved = useCase.execute(command);

        assertThat(saved.getOriginalName()).isEqualTo("foto.jpg");
        assertThat(saved.getType()).isEqualTo(AnnouncementFileType.IMAGE);
        assertThat(saved.getFileStatus()).isEqualTo(AnnouncementFileStatus.PENDING);
        assertThat(saved.getProcessedS3Key()).isNull();
        assertThat(saved.getS3Key()).contains("/raw/");
        verify(storagePort).upload(anyString(), eq("image/jpeg"), eq(command.content()));
        verify(fileRepository).save(any(AnnouncementFile.class));
    }

    @Test
    void shouldMarkReadyImmediatelyWhenStorageIsSync() {
        mockContext(UserType.SENAI, userId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(fileRepository.countByAnnouncementId(announcementId)).thenReturn(0L);
        when(storagePort.upload(anyString(), anyString(), any()))
                .thenAnswer(inv -> StorageUploadResult.sync(inv.getArgument(0), "local"));
        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AnnouncementFile saved = useCase.execute(command);

        assertThat(saved.getFileStatus()).isEqualTo(AnnouncementFileStatus.READY);
        assertThat(saved.getProcessedS3Key()).isEqualTo(saved.getS3Key());
    }

    @Test
    void shouldClearExistingThumbnailWhenNewFileIsMarkedAsThumbnail() {
        AttachAnnouncementFileCommand thumbnailCommand = new AttachAnnouncementFileCommand(
                announcementId, "capa.jpg", "image/jpeg", 1024L, new byte[]{1}, true, userId
        );

        mockContext(UserType.SENAI, userId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(fileRepository.countByAnnouncementId(announcementId)).thenReturn(0L);
        when(storagePort.upload(anyString(), anyString(), any()))
                .thenReturn(StorageUploadResult.async("key-123", "bucket"));
        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(thumbnailCommand);

        verify(fileRepository).clearAllThumbnailsByAnnouncementId(announcementId);
    }

    @Test
    void shouldNotClearThumbnailWhenFileIsNotMarkedAsThumbnail() {
        mockContext(UserType.SENAI, userId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(fileRepository.countByAnnouncementId(announcementId)).thenReturn(0L);
        when(storagePort.upload(anyString(), anyString(), any()))
                .thenReturn(StorageUploadResult.async("key-123", "bucket"));
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

        verify(storagePort, never()).upload(anyString(), anyString(), any());
    }

    @Test
    void shouldThrowNotFoundWhenStudentUploads() {
        mockContext(UserType.STUDENT, userId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(AnnouncementNotFoundException.class);

        verify(storagePort, never()).upload(anyString(), anyString(), any());
    }

    @Test
    void shouldThrowNotFoundWhenTeacherIsNotOwner() {
        UUID otherUserId = UUID.randomUUID();
        mockContext(UserType.TEACHER, otherUserId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(AnnouncementNotFoundException.class);
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

        verify(storagePort, never()).upload(anyString(), anyString(), any());
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

        verify(storagePort, never()).upload(anyString(), anyString(), any());
    }

    @Test
    void shouldDeleteUploadedFileFromStorageWhenTransactionRollsBack() {
        mockContext(UserType.SENAI, userId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(fileRepository.countByAnnouncementId(announcementId)).thenReturn(0L);
        when(storagePort.upload(anyString(), anyString(), any()))
                .thenAnswer(inv -> StorageUploadResult.async(inv.getArgument(0), "bucket"));
        when(fileRepository.save(any())).thenThrow(new RuntimeException("falha ao persistir"));

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(RuntimeException.class);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }
            verify(storagePort).delete(keyCaptor.capture(), eq("bucket"));
            assertThat(keyCaptor.getValue()).contains("/raw/");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void mockContext(UserType userType, UUID id) {
        when(contextProvider.getRequestContext()).thenReturn(new RequestContext(id, userType, List.of()));
    }
}
