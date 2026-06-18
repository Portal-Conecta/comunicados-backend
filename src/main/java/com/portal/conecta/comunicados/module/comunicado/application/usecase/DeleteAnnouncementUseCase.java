package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.portal.conecta.comunicados.module.comunicado.application.command.RemoveAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementPermissionDeniedException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteAnnouncementUseCase {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementHistoryRepository historyRepository;
    private final RequestContextProvider contextProvider;
    private final AnnouncementPermissionValidator permissionValidator;

    @Transactional
    public void execute(UUID id) {
        RequestContext context = contextProvider.getRequestContext();
        Announcement announcement = announcementRepository.findByIdAndRemovedAtIsNull(id)
                .orElseThrow(AnnouncementNotFoundException::new);

        if (!permissionValidator.canDelete(context.userType(), context.userId(), announcement)) {
            throw new AnnouncementPermissionDeniedException("Usuário não tem permissão para remover este comunicado.");
        }

        Instant now = Instant.now();
        RemoveAnnouncementCommand command = RemoveAnnouncementCommand.of(id, context.userId());

        announcementRepository.save(command.toEntity(announcement, now));
        historyRepository.save(command.toRemovalHistory(announcement, now));
    }
}
