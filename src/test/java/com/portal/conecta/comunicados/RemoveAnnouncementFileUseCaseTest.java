package com.portal.conecta.comunicados;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.portal.conecta.comunicados.module.comunicado.application.command.RemoveAnnouncementFileCommand;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.RemoveAnnouncementFileUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementFileNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementPermissionDeniedException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementFileRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StoragePort;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubClassPort;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import com.portal.conecta.comunicados.shared.context.UserType;

@ExtendWith(MockitoExtension.class)
class RemoveAnnouncementFileUseCaseTest {

    @Mock private AnnouncementFileRepository fileRepository;
    @Mock private RequestContextProvider contextProvider;
    @Mock private StoragePort storagePort;

    @Spy
    private AnnouncementPermissionValidator permissionValidator =
            new AnnouncementPermissionValidator(org.mockito.Mockito.mock(HubClassPort.class));

    @InjectMocks
    private RemoveAnnouncementFileUseCase useCase;

    private UUID fileId;
    private UUID ownerId;
    private AnnouncementFile file;

    @BeforeEach
    void setUp() {
        fileId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        Announcement announcement = Announcement.builder()
                .id(UUID.randomUUID())
                .title("Comunicado")
                .description("Descrição")
                .origin(AnnouncementOrigin.SENAI)
                .status(AnnouncementStatus.PUBLISHED)
                .pinned(false)
                .createdByUserId(ownerId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        file = AnnouncementFile.builder()
                .id(fileId)
                .announcement(announcement)
                .originalName("foto.jpg")
                .s3Key("s3-key-abc")
                .s3Bucket("bucket")
                .contentType("image/jpeg")
                .sizeBytes(1024L)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void shouldDeleteFromStorageAndDatabaseSuccessfully() {
        mockContext(UserType.SENAI, ownerId);
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));

        useCase.execute(RemoveAnnouncementFileCommand.of(fileId, ownerId));

        verify(storagePort).delete("s3-key-abc", "bucket");
        verify(fileRepository).delete(file);
    }

    @Test
    void shouldThrowNotFoundWhenFileDoesNotExist() {
        mockContext(UserType.SENAI, ownerId);
        when(fileRepository.findById(fileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(RemoveAnnouncementFileCommand.of(fileId, ownerId)))
                .isInstanceOf(AnnouncementFileNotFoundException.class);

        verify(storagePort, never()).delete(any(), any());
    }

    @Test
    void shouldThrowPermissionDeniedWhenCallerCannotEditAnnouncement() {
        UUID otherId = UUID.randomUUID();
        mockContext(UserType.TEACHER, otherId);
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));

        assertThatThrownBy(() -> useCase.execute(RemoveAnnouncementFileCommand.of(fileId, otherId)))
                .isInstanceOf(AnnouncementPermissionDeniedException.class);

        verify(storagePort, never()).delete(any(), any());
        verify(fileRepository, never()).delete(any());
    }

    private void mockContext(UserType userType, UUID id) {
        when(contextProvider.getRequestContext()).thenReturn(new RequestContext(id, userType, List.of()));
    }
}
