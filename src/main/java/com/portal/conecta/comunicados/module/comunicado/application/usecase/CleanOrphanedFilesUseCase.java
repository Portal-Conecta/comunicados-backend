package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementFileRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StoragePort;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.storage.StorageProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CleanOrphanedFilesUseCase {

    private final AnnouncementFileRepository fileRepository;
    private final StoragePort storagePort;
    private final StorageProperties storageProperties;

    @Transactional
    public int execute(int maxAgeHours) {
        Instant cutoff = Instant.now().minus(maxAgeHours, ChronoUnit.HOURS);
        List<AnnouncementFile> expired = fileRepository
                .findByFileStatusAndCreatedAtBefore(AnnouncementFileStatus.PENDING, cutoff);

        int count = 0;
        for (AnnouncementFile file : expired) {
            String processedKey = deriveProcessedKey(file.getS3Key());
            boolean existsInBucketB = storagePort
                    .headObject(storageProperties.filesBucket(), processedKey)
                    .isPresent();

            if (!existsInBucketB) {
                file.setFileStatus(AnnouncementFileStatus.FAILED);
                fileRepository.save(file);
                count++;
            }
        }
        return count;
    }

    // "comunicados/{ownerId}/raw/{fileId}.ext" -> "comunicados/{ownerId}/processed/{fileId}"
    private String deriveProcessedKey(String rawKey) {
        String withoutExtension = rawKey.replaceAll("\\.[^./]+$", "");
        return withoutExtension.replace("/raw/", "/processed/");
    }
}
