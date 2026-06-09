package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import com.portal.conecta.comunicados.module.comunicado.application.command.CreateAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementPermissionDeniedException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementDestinationRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CreateAnnouncementUseCase {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementDestinationRepository destinationRepository;
    private final AnnouncementHistoryRepository historyRepository;
    private final RequestContextProvider contextProvider;
    private final AnnouncementPermissionValidator permissionValidator;


    @Transactional
    public Announcement execute(CreateAnnouncementCommand command) {
        RequestContext context = contextProvider.getRequestContext();

        if (!permissionValidator.canCreate(context.userType())) {
            throw new AnnouncementPermissionDeniedException();
        }

        Instant now = Instant.now();
        Announcement announcement = Announcement.builder()
                .title(command.data().title())
                .description(command.data().description())
                .origin(command.data().origin())
                .status(command.data().status())
                .pinned(command.data().pinned() != null && command.data().pinned())
                .pinnedOrder(command.data().pinnedOrder())
                .scheduledFor(command.data().scheduledFor())
                .createdByUserId(command.createdByUserId())
                .createdAt(now)
                .updatedAt(now)
                .build();

        announcement = announcementRepository.save(announcement);

        Announcement finalAnnouncement = announcement;
        List<AnnouncementDestination> destinations = command.data().destinations()
                .stream()
                .map(destReq -> AnnouncementDestination.builder()
                        .announcement(finalAnnouncement)
                        .type(destReq.type())
                        .referenceId(destReq.referenceId())
                        .build())
                .toList();

        destinationRepository.saveAll(destinations);

        AnnouncementHistory history = AnnouncementHistory.builder()
                .announcement(announcement)
                .action(AnnouncementHistoryAction.CREATION)
                .userId(command.createdByUserId())
                .createdAt(now)
                .build();

        historyRepository.save(history);

        return announcement;
    }

}