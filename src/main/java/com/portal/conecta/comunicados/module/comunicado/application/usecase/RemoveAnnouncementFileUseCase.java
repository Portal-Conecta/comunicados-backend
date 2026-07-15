package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portal.conecta.comunicados.module.comunicado.application.command.RemoveAnnouncementFileCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementFileNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementFileRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StoragePort;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RemoveAnnouncementFileUseCase {

    private final AnnouncementFileRepository fileRepository;
    private final RequestContextProvider contextProvider;
    private final AnnouncementPermissionValidator permissionValidator;
    private final StoragePort storagePort;

    @Transactional
    public void execute(RemoveAnnouncementFileCommand command) {
        RequestContext context = contextProvider.getRequestContext();

        AnnouncementFile file = fileRepository.findById(command.fileId())
                .orElseThrow(AnnouncementFileNotFoundException::new);

        if (!permissionValidator.canUpdate(context.userType(), context.userId(), file.getAnnouncement())) {
            throw new AnnouncementNotFoundException();
        }

        storagePort.delete(file.getS3Key(), file.getS3Bucket());
        fileRepository.delete(file);
    }
}
