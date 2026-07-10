package com.portal.conecta.comunicados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.portal.conecta.comunicados.module.comunicado.application.dto.AnnouncementFileView;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.ListAnnouncementFilesUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileType;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementFileRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StorageObjectMetadata;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StoragePort;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.storage.StorageProperties;

@ExtendWith(MockitoExtension.class)
class ListAnnouncementFilesUseCaseTest {

    @Mock private AnnouncementRepository announcementRepository;
    @Mock private AnnouncementFileRepository fileRepository;
    @Mock private StoragePort storagePort;
    @Mock private StorageProperties storageProperties;

    private ListAnnouncementFilesUseCase useCase;

    private UUID announcementId;
    private UUID userId;
    private Announcement announcement;

    @BeforeEach
    void setUp() {
        useCase = new ListAnnouncementFilesUseCase(
                announcementRepository, fileRepository, storagePort, storageProperties
        );
        announcementId = UUID.randomUUID();
        userId = UUID.randomUUID();
        announcement = Announcement.builder().id(announcementId).build();
    }

    @Test
    void shouldThrowNotFoundWhenAnnouncementDoesNotExist() {
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(announcementId))
                .isInstanceOf(AnnouncementNotFoundException.class);
    }

    @Test
    void shouldReconcilePendingFileToReadyWhenObjectExistsInS3() {
        UUID fileId = UUID.randomUUID();
        AnnouncementFile pending = pendingFile(fileId, "comunicados/" + userId + "/raw/" + fileId + ".jpg", "comunicados-raw-sa");

        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(fileRepository.findByAnnouncementId(announcementId)).thenReturn(List.of(pending));
        when(storageProperties.filesBucket()).thenReturn("comunicados-processed-sa");
        when(storagePort.headObject(eq("comunicados-processed-sa"), eq("comunicados/" + userId + "/processed/" + fileId)))
                .thenReturn(Optional.of(new StorageObjectMetadata(80_000L, 1200, 800, "image/webp")));
        when(storagePort.presignDownload(eq("comunicados-processed-sa"), any(), any(Duration.class)))
                .thenReturn("https://s3.example.com/signed");

        List<AnnouncementFileView> result = useCase.execute(announcementId);

        assertThat(result).hasSize(1);
        AnnouncementFileView view = result.get(0);
        assertThat(view.file().getFileStatus()).isEqualTo(AnnouncementFileStatus.READY);
        assertThat(view.file().getProcessedS3Key()).isEqualTo("comunicados/" + userId + "/processed/" + fileId);
        assertThat(view.file().getSizeBytes()).isEqualTo(80_000L);
        assertThat(view.file().getContentType()).isEqualTo("image/webp");
        assertThat(view.displayUrl()).isEqualTo("https://s3.example.com/signed");
        verify(fileRepository).save(pending);
    }

    @Test
    void shouldKeepPendingAndReturnNullUrlWhenObjectNotInS3() {
        UUID fileId = UUID.randomUUID();
        AnnouncementFile pending = pendingFile(fileId, "comunicados/" + userId + "/raw/" + fileId + ".jpg", "comunicados-raw-sa");

        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(fileRepository.findByAnnouncementId(announcementId)).thenReturn(List.of(pending));
        when(storageProperties.filesBucket()).thenReturn("comunicados-processed-sa");
        when(storagePort.headObject(any(), any())).thenReturn(Optional.empty());

        List<AnnouncementFileView> result = useCase.execute(announcementId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).file().getFileStatus()).isEqualTo(AnnouncementFileStatus.PENDING);
        assertThat(result.get(0).displayUrl()).isNull();
        verify(fileRepository, never()).save(any());
    }

    @Test
    void shouldGenerateDisplayUrlFromProcessedKeyForReadyFile() {
        UUID fileId = UUID.randomUUID();
        AnnouncementFile ready = readyFileWithProcessedKey(fileId, "comunicados/" + userId + "/processed/" + fileId);

        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(fileRepository.findByAnnouncementId(announcementId)).thenReturn(List.of(ready));
        when(storageProperties.filesBucket()).thenReturn("comunicados-processed-sa");
        when(storagePort.presignDownload(eq("comunicados-processed-sa"), eq("comunicados/" + userId + "/processed/" + fileId), any(Duration.class)))
                .thenReturn("https://s3.example.com/processed-signed");

        List<AnnouncementFileView> result = useCase.execute(announcementId);

        assertThat(result.get(0).displayUrl()).isEqualTo("https://s3.example.com/processed-signed");
    }

    @Test
    void shouldReturnNullDisplayUrlForReadyFileWithoutProcessedKey() {
        UUID fileId = UUID.randomUUID();
        AnnouncementFile ready = readyFileWithoutProcessedKey(fileId, "comunicados/" + userId + "/raw/" + fileId + ".pdf", "comunicados-processed-sa");

        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(fileRepository.findByAnnouncementId(announcementId)).thenReturn(List.of(ready));

        List<AnnouncementFileView> result = useCase.execute(announcementId);

        assertThat(result.get(0).displayUrl()).isNull();
        verify(storagePort, never()).presignDownload(any(), any(), any());
    }

    @Test
    void shouldNotCallHeadObjectForReadyFiles() {
        UUID fileId = UUID.randomUUID();
        AnnouncementFile ready = readyFileWithProcessedKey(fileId, "comunicados/" + userId + "/processed/" + fileId);

        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(fileRepository.findByAnnouncementId(announcementId)).thenReturn(List.of(ready));
        when(storageProperties.filesBucket()).thenReturn("comunicados-processed-sa");
        when(storagePort.presignDownload(any(), any(), any())).thenReturn("https://s3.example.com/signed");

        useCase.execute(announcementId);

        verify(storagePort, never()).headObject(any(), any());
    }

    private AnnouncementFile pendingFile(UUID id, String s3Key, String s3Bucket) {
        return AnnouncementFile.builder()
                .id(id)
                .originalName("foto.jpg")
                .s3Key(s3Key)
                .s3Bucket(s3Bucket)
                .contentType("image/jpeg")
                .type(AnnouncementFileType.IMAGE)
                .fileStatus(AnnouncementFileStatus.PENDING)
                .sizeBytes(100_000L)
                .isThumbnail(false)
                .uploadedByUserId(userId)
                .createdAt(Instant.now())
                .build();
    }

    private AnnouncementFile readyFileWithProcessedKey(UUID id, String processedKey) {
        return AnnouncementFile.builder()
                .id(id)
                .originalName("foto.webp")
                .s3Key("comunicados/" + userId + "/raw/" + id + ".jpg")
                .s3Bucket("comunicados-raw-sa")
                .processedS3Key(processedKey)
                .contentType("image/jpeg")
                .type(AnnouncementFileType.IMAGE)
                .fileStatus(AnnouncementFileStatus.READY)
                .sizeBytes(80_000L)
                .isThumbnail(false)
                .uploadedByUserId(userId)
                .createdAt(Instant.now())
                .build();
    }

    private AnnouncementFile readyFileWithoutProcessedKey(UUID id, String s3Key, String s3Bucket) {
        return AnnouncementFile.builder()
                .id(id)
                .originalName("doc.pdf")
                .s3Key(s3Key)
                .s3Bucket(s3Bucket)
                .contentType("application/pdf")
                .type(AnnouncementFileType.DOCUMENT)
                .fileStatus(AnnouncementFileStatus.READY)
                .sizeBytes(500_000L)
                .isThumbnail(false)
                .uploadedByUserId(userId)
                .createdAt(Instant.now())
                .build();
    }
}
