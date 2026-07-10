package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import com.portal.conecta.comunicados.module.comunicado.application.command.RescheduleAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementMustBeInTheFutureException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementPermissionDeniedException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.time.Instant;

import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotScheduledException;

@Component
@RequiredArgsConstructor
public class RescheduleAnnouncementUseCase {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementHistoryRepository historyRepository;
    private final RequestContextProvider contextProvider;
    private final AnnouncementPermissionValidator permissionValidator;

    @Transactional
    public Announcement execute(RescheduleAnnouncementCommand command) {
        RequestContext context = contextProvider.getRequestContext();

        Announcement announcement = announcementRepository.findById(command.announcementId())
                .filter(a -> a.getRemovedAt() == null)
                .orElseThrow(AnnouncementNotFoundException::new);

        if (!permissionValidator.canReschedule(announcement, context)) {
            throw new AnnouncementPermissionDeniedException();
        }

        // RN-REA-01: só SCHEDULED
        if (announcement.getStatus() != AnnouncementStatus.SCHEDULED) {
            throw new AnnouncementNotScheduledException();
        }

        Instant now = Instant.now();
        if (!command.scheduledFor().isAfter(now)) {
            throw new AnnouncementMustBeInTheFutureException();
        }

        announcement = command.applyTo(announcement, now);
        announcement = announcementRepository.save(announcement);

        AnnouncementHistory history = AnnouncementHistory.builder()
                .announcement(announcement)
                .action(AnnouncementHistoryAction.EDIT)
                .userId(context.userId())
                .createdAt(now)
                .build();

        historyRepository.save(history);

        return announcement;
    }
}
