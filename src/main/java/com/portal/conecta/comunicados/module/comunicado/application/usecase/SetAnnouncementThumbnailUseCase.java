package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portal.conecta.comunicados.module.comunicado.application.command.SetAnnouncementThumbnailCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementFileNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementFileRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SetAnnouncementThumbnailUseCase {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementFileRepository fileRepository;
    private final RequestContextProvider contextProvider;
    private final AnnouncementPermissionValidator permissionValidator;

    @Transactional
    public AnnouncementFile execute(SetAnnouncementThumbnailCommand command) {
        RequestContext context = contextProvider.getRequestContext();

        Announcement announcement = announcementRepository
                .findByIdAndRemovedAtIsNull(command.announcementId())
                .orElseThrow(AnnouncementNotFoundException::new);

        if (!permissionValidator.canUpdate(context.userType(), context.userId(), announcement)) {
            throw new AnnouncementNotFoundException();
        }

        AnnouncementFile file = fileRepository.findById(command.fileId())
                .filter(f -> f.getAnnouncement().getId().equals(command.announcementId()))
                .orElseThrow(AnnouncementFileNotFoundException::new);

        fileRepository.clearAllThumbnailsByAnnouncementId(command.announcementId());
        return fileRepository.save(command.toEntity(file));
    }
}
