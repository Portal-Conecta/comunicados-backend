package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import com.portal.conecta.comunicados.module.comunicado.application.command.UnpinAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
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
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UnpinAnnouncementUseCase {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementHistoryRepository historyRepository;
    private final RequestContextProvider contextProvider;
    private final AnnouncementPermissionValidator permissionValidator;

    @Transactional
    public Announcement execute(UnpinAnnouncementCommand command) {
        RequestContext context = contextProvider.getRequestContext();

        if(!permissionValidator.canPin(context.userType())) {
            throw new AnnouncementPermissionDeniedException();
        }

        var announcement = announcementRepository.findByIdAndRemovedAtIsNull(command.announcementId())
                .orElseThrow(AnnouncementNotFoundException::new);

        announcement.setPinned(false);
        announcement.setPinnedOrder(null);

        announcement = announcementRepository.save(announcement);

        recordHistory(announcement, command.unpinnedByUserId(), "UNPINNED");

        return announcement;
    }

    private void recordHistory(Announcement announcement, UUID userId, String action) {
        AnnouncementHistory history = AnnouncementHistory.builder()
                .announcement(announcement)
                .userId(userId)
                .action(AnnouncementHistoryAction.valueOf(action))
                .createdAt(Instant.now())
                .build();

        historyRepository.save(history);
    }
}