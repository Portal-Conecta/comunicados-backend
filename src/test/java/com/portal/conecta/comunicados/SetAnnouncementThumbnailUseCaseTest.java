package com.portal.conecta.comunicados;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.portal.conecta.comunicados.module.comunicado.application.command.SetAnnouncementThumbnailCommand;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.SetAnnouncementThumbnailUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementFileNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementFileRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubClassPort;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import com.portal.conecta.comunicados.shared.context.UserType;

@ExtendWith(MockitoExtension.class)
class SetAnnouncementThumbnailUseCaseTest {

    @Mock private AnnouncementRepository announcementRepository;
    @Mock private AnnouncementFileRepository fileRepository;
    @Mock private RequestContextProvider contextProvider;

    @Spy
    private AnnouncementPermissionValidator permissionValidator =
            new AnnouncementPermissionValidator(org.mockito.Mockito.mock(HubClassPort.class));

    @InjectMocks
    private SetAnnouncementThumbnailUseCase useCase;

    private UUID announcementId;
    private UUID fileId;
    private UUID userId;
    private Announcement announcement;
    private AnnouncementFile file;

    @BeforeEach
    void setUp() {
        announcementId = UUID.randomUUID();
        fileId = UUID.randomUUID();
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

        file = AnnouncementFile.builder()
                .id(fileId)
                .announcement(announcement)
                .originalName("capa.jpg")
                .s3Key("key-abc")
                .s3Bucket("bucket")
                .contentType("image/jpeg")
                .sizeBytes(2048L)
                .isThumbnail(false)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void shouldClearOldThumbnailAndSetNewOne() {
        mockContext(UserType.SENAI, userId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AnnouncementFile result = useCase.execute(SetAnnouncementThumbnailCommand.of(announcementId, fileId, userId));

        verify(fileRepository).clearAllThumbnailsByAnnouncementId(announcementId);
        assertThat(result.isThumbnail()).isTrue();
    }

    @Test
    void shouldThrowFileNotFoundWhenFileDoesNotBelongToAnnouncement() {
        UUID otherAnnouncementId = UUID.randomUUID();
        Announcement otherAnnouncement = Announcement.builder()
                .id(otherAnnouncementId)
                .createdByUserId(userId)
                .status(AnnouncementStatus.PUBLISHED)
                .build();

        AnnouncementFile fileFromOther = AnnouncementFile.builder()
                .id(fileId)
                .announcement(otherAnnouncement)
                .build();

        mockContext(UserType.SENAI, userId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(fileFromOther));

        assertThatThrownBy(() -> useCase.execute(SetAnnouncementThumbnailCommand.of(announcementId, fileId, userId)))
                .isInstanceOf(AnnouncementFileNotFoundException.class);

        verify(fileRepository, never()).clearAllThumbnailsByAnnouncementId(any());
    }

    @Test
    void shouldThrowPermissionDeniedWhenCallerCannotEditAnnouncement() {
        UUID otherId = UUID.randomUUID();
        mockContext(UserType.TEACHER, otherId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));

        assertThatThrownBy(() -> useCase.execute(SetAnnouncementThumbnailCommand.of(announcementId, fileId, otherId)))
                .isInstanceOf(AnnouncementNotFoundException.class);

        verify(fileRepository, never()).clearAllThumbnailsByAnnouncementId(any());
    }

    private void mockContext(UserType userType, UUID id) {
        when(contextProvider.getRequestContext()).thenReturn(new RequestContext(id, userType, List.of()));
    }
}
