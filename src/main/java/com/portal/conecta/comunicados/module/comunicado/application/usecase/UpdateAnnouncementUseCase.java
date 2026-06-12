package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.conecta.comunicados.module.comunicado.application.command.UpdateAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementConflictException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementPermissionDeniedException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementDestinationRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateAnnouncementUseCase {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementDestinationRepository destinationRepository;
    private final AnnouncementHistoryRepository historyRepository;
    private final RequestContextProvider contextProvider;
    private final AnnouncementPermissionValidator permissionValidator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Announcement execute(UpdateAnnouncementCommand command) {
        RequestContext context = contextProvider.getRequestContext();
        Announcement announcement = announcementRepository.findById(command.id())
                .orElseThrow(AnnouncementNotFoundException::new);

        if (announcement.getRemovedAt() != null || announcement.getStatus() == AnnouncementStatus.REMOVED) {
            throw new AnnouncementConflictException("Removed announcements cannot be edited.");
        }

        if (!permissionValidator.canUpdate(context.userType(), context.userId(), announcement)) {
            throw new AnnouncementPermissionDeniedException();
        }

        Instant now = Instant.now();
        List<AnnouncementDestination> currentDestinations = destinationRepository.findByAnnouncementId(command.id());
        String snapshot = announcement.getStatus() == AnnouncementStatus.PUBLISHED
                ? command.toSnapshot(announcement, currentDestinations, objectMapper)
                : null;

        Announcement updated = command.toEntity(announcement, now);
        updated = announcementRepository.save(updated);

        destinationRepository.deleteByAnnouncementId(updated.getId());

        List<AnnouncementDestination> destinations = command.toDestinations(updated);
        destinationRepository.saveAll(destinations);
        updated.setDestinations(destinations);

        historyRepository.save(command.toEditHistory(updated, now, snapshot));

        return updated;
    }

}