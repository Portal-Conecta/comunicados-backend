package com.portal.conecta.comunicados;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileType;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementFileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AnnouncementFilePersistenceTest {

    @Autowired
    private AnnouncementFileRepository fileRepository;

    @Autowired
    private TestEntityManager em;

    private Announcement persistAnnouncement() {
        Instant now = Instant.now();
        Announcement announcement = Announcement.builder()
                .title("Aviso")
                .description("Descrição")
                .origin(AnnouncementOrigin.BOTH)
                .status(AnnouncementStatus.PUBLISHED)
                .pinned(false)
                .createdByUserId(UUID.randomUUID())
                .createdAt(now)
                .updatedAt(now)
                .build();
        return em.persistFlushFind(announcement);
    }

    private AnnouncementFile newFile(Announcement announcement, AnnouncementFileStatus status, String processedS3Key) {
        return AnnouncementFile.builder()
                .announcement(announcement)
                .originalName("foto.jpg")
                .s3Key("announcement/" + announcement.getId() + "/raw/" + UUID.randomUUID() + ".jpg")
                .s3Bucket("comunicados-raw")
                .processedS3Key(processedS3Key)
                .contentType("image/jpeg")
                .type(AnnouncementFileType.IMAGE)
                .fileStatus(status)
                .sizeBytes(2048L)
                .isThumbnail(false)
                .uploadedByUserId(UUID.randomUUID())
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void persistsPendingFileWithoutProcessedKey() {
        Announcement announcement = persistAnnouncement();

        AnnouncementFile saved = fileRepository.save(newFile(announcement, AnnouncementFileStatus.PENDING, null));
        em.flush();
        em.clear();

        AnnouncementFile reloaded = fileRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getFileStatus()).isEqualTo(AnnouncementFileStatus.PENDING);
        assertThat(reloaded.getProcessedS3Key()).isNull();
    }

    @Test
    void persistsReadyFileWithProcessedKey() {
        Announcement announcement = persistAnnouncement();
        String processedS3Key = "announcement/" + announcement.getId() + "/processed/" + UUID.randomUUID();

        AnnouncementFile saved = fileRepository.save(newFile(announcement, AnnouncementFileStatus.READY, processedS3Key));
        em.flush();
        em.clear();

        AnnouncementFile reloaded = fileRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getFileStatus()).isEqualTo(AnnouncementFileStatus.READY);
        assertThat(reloaded.getProcessedS3Key()).isEqualTo(processedS3Key);
    }

    @Test
    void allowsTransitionFromPendingToReady() {
        Announcement announcement = persistAnnouncement();
        AnnouncementFile saved = fileRepository.save(newFile(announcement, AnnouncementFileStatus.PENDING, null));
        em.flush();

        String processedS3Key = "announcement/" + announcement.getId() + "/processed/" + UUID.randomUUID();
        saved.setFileStatus(AnnouncementFileStatus.READY);
        saved.setProcessedS3Key(processedS3Key);
        fileRepository.save(saved);
        em.flush();
        em.clear();

        AnnouncementFile reloaded = fileRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getFileStatus()).isEqualTo(AnnouncementFileStatus.READY);
        assertThat(reloaded.getProcessedS3Key()).isEqualTo(processedS3Key);
    }
}
