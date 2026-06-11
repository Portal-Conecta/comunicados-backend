package com.portal.conecta.comunicados.module.comunicado.application.usecase;



import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.application.command.CreateAnnouncementCommand;
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
        Announcement announcement = command.toEntity(now);

        announcement = announcementRepository.save(announcement);

        List<AnnouncementDestination> destinations = command.toDestinations(announcement);

        destinationRepository.saveAll(destinations);

        AnnouncementHistory history = command.toCreationHistory(announcement, now);

        historyRepository.save(history);

        return announcement;
    }
}

