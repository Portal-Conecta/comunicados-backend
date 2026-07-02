package com.portal.conecta.comunicados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.portal.conecta.comunicados.module.comunicado.application.usecase.CleanOrphanedFilesUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileType;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementFileRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StorageObjectMetadata;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StoragePort;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.storage.StorageProperties;

@ExtendWith(MockitoExtension.class)
class CleanOrphanedFilesUseCaseTest {

    @Mock private AnnouncementFileRepository fileRepository;
    @Mock private StoragePort storagePort;
    @Mock private StorageProperties storageProperties;

    private CleanOrphanedFilesUseCase useCase;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new CleanOrphanedFilesUseCase(fileRepository, storagePort, storageProperties);
    }

    @Test
    void shouldMarkFileAsFailedWhenExpiredAndNotFoundInBucketB() {
        UUID fileId = UUID.randomUUID();
        AnnouncementFile file = pendingFile(fileId, hoursAgo(25));

        when(fileRepository.findByFileStatusAndCreatedAtBefore(eq(AnnouncementFileStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(file));
        when(storageProperties.filesBucket()).thenReturn("comunicados-processed-sa");
        when(storagePort.headObject(eq("comunicados-processed-sa"), eq("comunicados/" + userId + "/processed/" + fileId)))
                .thenReturn(Optional.empty());

        int count = useCase.execute(24);

        assertThat(count).isEqualTo(1);
        assertThat(file.getFileStatus()).isEqualTo(AnnouncementFileStatus.FAILED);
        verify(fileRepository).save(file);
    }

    @Test
    void shouldNotMarkFileAsFailedWhenAlreadyProcessedInBucketB() {
        UUID fileId = UUID.randomUUID();
        AnnouncementFile file = pendingFile(fileId, hoursAgo(25));

        when(fileRepository.findByFileStatusAndCreatedAtBefore(eq(AnnouncementFileStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(file));
        when(storageProperties.filesBucket()).thenReturn("comunicados-processed-sa");
        when(storagePort.headObject(eq("comunicados-processed-sa"), eq("comunicados/" + userId + "/processed/" + fileId)))
                .thenReturn(Optional.of(new StorageObjectMetadata(80_000L, 1200, 800)));

        int count = useCase.execute(24);

        assertThat(count).isEqualTo(0);
        assertThat(file.getFileStatus()).isEqualTo(AnnouncementFileStatus.PENDING);
        verify(fileRepository, never()).save(any());
    }

    @Test
    void shouldReturnZeroWhenNoExpiredFilesExist() {
        when(fileRepository.findByFileStatusAndCreatedAtBefore(any(), any())).thenReturn(List.of());

        int count = useCase.execute(24);

        assertThat(count).isEqualTo(0);
        verify(storagePort, never()).headObject(any(), any());
        verify(fileRepository, never()).save(any());
    }

    @Test
    void shouldProcessMultipleFilesAndCountOnlyFailed() {
        UUID fileId1 = UUID.randomUUID();
        UUID fileId2 = UUID.randomUUID();
        UUID fileId3 = UUID.randomUUID();

        AnnouncementFile orphan1 = pendingFile(fileId1, hoursAgo(30));
        AnnouncementFile orphan2 = pendingFile(fileId2, hoursAgo(48));
        AnnouncementFile stillProcessing = pendingFile(fileId3, hoursAgo(25));

        when(fileRepository.findByFileStatusAndCreatedAtBefore(eq(AnnouncementFileStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(orphan1, orphan2, stillProcessing));
        when(storageProperties.filesBucket()).thenReturn("comunicados-processed-sa");
        when(storagePort.headObject(eq("comunicados-processed-sa"), eq("comunicados/" + userId + "/processed/" + fileId1)))
                .thenReturn(Optional.empty());
        when(storagePort.headObject(eq("comunicados-processed-sa"), eq("comunicados/" + userId + "/processed/" + fileId2)))
                .thenReturn(Optional.empty());
        when(storagePort.headObject(eq("comunicados-processed-sa"), eq("comunicados/" + userId + "/processed/" + fileId3)))
                .thenReturn(Optional.of(new StorageObjectMetadata(50_000L, null, null)));

        int count = useCase.execute(24);

        assertThat(count).isEqualTo(2);
        assertThat(orphan1.getFileStatus()).isEqualTo(AnnouncementFileStatus.FAILED);
        assertThat(orphan2.getFileStatus()).isEqualTo(AnnouncementFileStatus.FAILED);
        assertThat(stillProcessing.getFileStatus()).isEqualTo(AnnouncementFileStatus.PENDING);
        verify(fileRepository).save(orphan1);
        verify(fileRepository).save(orphan2);
        verify(fileRepository, never()).save(stillProcessing);
    }

    @Test
    void shouldDeriveCorrectProcessedKeyFromRawKey() {
        UUID fileId = UUID.randomUUID();
        String rawKey = "comunicados/" + userId + "/raw/" + fileId + ".png";
        String expectedProcessedKey = "comunicados/" + userId + "/processed/" + fileId;

        AnnouncementFile file = AnnouncementFile.builder()
                .id(fileId)
                .originalName("foto.png")
                .s3Key(rawKey)
                .s3Bucket("comunicados-raw-sa")
                .contentType("image/png")
                .type(AnnouncementFileType.IMAGE)
                .fileStatus(AnnouncementFileStatus.PENDING)
                .sizeBytes(100_000L)
                .isThumbnail(false)
                .uploadedByUserId(userId)
                .createdAt(hoursAgo(25))
                .build();

        when(fileRepository.findByFileStatusAndCreatedAtBefore(any(), any())).thenReturn(List.of(file));
        when(storageProperties.filesBucket()).thenReturn("comunicados-processed-sa");
        when(storagePort.headObject(any(), any())).thenReturn(Optional.empty());

        useCase.execute(24);

        verify(storagePort).headObject("comunicados-processed-sa", expectedProcessedKey);
    }

    private AnnouncementFile pendingFile(UUID id, Instant createdAt) {
        return AnnouncementFile.builder()
                .id(id)
                .originalName("foto.jpg")
                .s3Key("comunicados/" + userId + "/raw/" + id + ".jpg")
                .s3Bucket("comunicados-raw-sa")
                .contentType("image/jpeg")
                .type(AnnouncementFileType.IMAGE)
                .fileStatus(AnnouncementFileStatus.PENDING)
                .sizeBytes(100_000L)
                .isThumbnail(false)
                .uploadedByUserId(userId)
                .createdAt(createdAt)
                .build();
    }

    private Instant hoursAgo(int hours) {
        return Instant.now().minus(hours, ChronoUnit.HOURS);
    }
}
