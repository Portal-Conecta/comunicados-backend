package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementFileRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListAnnouncementFilesUseCase {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementFileRepository fileRepository;

    @Transactional(readOnly = true)
    public List<AnnouncementFile> execute(UUID announcementId) {
        announcementRepository.findByIdAndRemovedAtIsNull(announcementId)
                .orElseThrow(AnnouncementNotFoundException::new);
        return fileRepository.findByAnnouncementId(announcementId);
    }
}
