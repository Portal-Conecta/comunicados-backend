package com.portal.conecta.comunicados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.portal.conecta.comunicados.module.comunicado.application.command.PresignUploadAnnouncementFileCommand;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.PresignUploadAnnouncementFileUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
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
import com.portal.conecta.comunicados.shared.context.UserType;

/**
 * Regressão do HTTP 500 no POST /presign: o use case pré-atribuía o ID da entidade, então o
 * {@code save()} do Spring Data virava merge()→UPDATE numa linha inexistente → StaleObjectStateException.
 * Este teste exercita a PERSISTÊNCIA REAL (H2) do use case — um mock de repositório não reproduziria
 * o bug, e era exatamente esse gap que deixava o 500 passar por CI.
 */
@SpringBootTest
@ActiveProfiles("test")
class PresignUploadAnnouncementFilePersistenceTest {

    @Autowired private PresignUploadAnnouncementFileUseCase useCase;
    @Autowired private AnnouncementRepository announcementRepository;
    @Autowired private AnnouncementFileRepository fileRepository;

    @MockitoBean private RequestContextProvider contextProvider;
    @MockitoBean private AnnouncementPermissionValidator permissionValidator;
    @MockitoBean private StoragePort storagePort;
    @MockitoBean private StorageProperties storageProperties;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(contextProvider.getRequestContext())
                .thenReturn(new RequestContext(userId, UserType.ADMIN, List.of()));
        when(permissionValidator.canUpdate(any(), any(), any())).thenReturn(true);
        when(storageProperties.maxFilesPerAnnouncement()).thenReturn(5);
        when(storageProperties.maxImageSizeBytes()).thenReturn(10L * 1024 * 1024);
        when(storagePort.presignUpload(anyString(), anyString(), anyLong()))
                .thenAnswer(invocation -> new PresignedUpload(
                        "https://s3.example.com/upload",
                        Map.of(),
                        invocation.getArgument(0),
                        "comunicados-raw-sa"));
    }

    @Test
    void persistsPendingFileAndReturnsRetrievableId() {
        Announcement announcement = persistPublishedAnnouncement();

        PresignUploadResponse response = useCase.execute(new PresignUploadAnnouncementFileCommand(
                announcement.getId(),
                "image/png",
                8L,
                "foto.png",
                false,
                userId));

        assertThat(response.fileId()).isNotNull();

        AnnouncementFile persisted = fileRepository.findById(response.fileId()).orElseThrow();
        assertThat(persisted.getFileStatus()).isEqualTo(AnnouncementFileStatus.PENDING);
        assertThat(persisted.getS3Key()).contains("/raw/").endsWith(".png");
    }

    private Announcement persistPublishedAnnouncement() {
        Instant now = Instant.now();
        return announcementRepository.save(Announcement.builder()
                .title("Aviso")
                .description("Descrição")
                .origin(AnnouncementOrigin.SENAI)
                .status(AnnouncementStatus.PUBLISHED)
                .pinned(false)
                .createdByUserId(userId)
                .createdByUserType(UserType.ADMIN)
                .publishedByUserId(userId)
                .publishedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
