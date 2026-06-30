package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementFileRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StoragePort;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.storage.StorageProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListAnnouncementFilesUseCase {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementFileRepository fileRepository;
    private final StoragePort storagePort;
    private final StorageProperties storageProperties;

    @Transactional
    public List<AnnouncementFile> execute(UUID announcementId) {
        announcementRepository.findByIdAndRemovedAtIsNull(announcementId)
                .orElseThrow(AnnouncementNotFoundException::new);

        List<AnnouncementFile> files = fileRepository.findByAnnouncementId(announcementId);
        files.stream()
                .filter(f -> f.getFileStatus() == AnnouncementFileStatus.PENDING)
                .forEach(this::reconcile);
        return files;
    }

    private void reconcile(AnnouncementFile file) {
        String processedKey = deriveProcessedKey(file.getS3Key());
        storagePort.headObject(storageProperties.filesBucket(), processedKey)
                .ifPresent(meta -> {
                    file.setFileStatus(AnnouncementFileStatus.READY);
                    file.setProcessedS3Key(processedKey);
                    file.setSizeBytes(meta.sizeBytes());
                    fileRepository.save(file);
                });
    }

    // "comunicados/{ownerId}/raw/{fileId}.ext" -> "comunicados/{ownerId}/processed/{fileId}"
    private String deriveProcessedKey(String rawKey) {
        String withoutExtension = rawKey.replaceAll("\\.[^./]+$", "");
        return withoutExtension.replace("/raw/", "/processed/");
    }
}
