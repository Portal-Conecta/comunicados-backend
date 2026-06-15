package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portal.conecta.comunicados.module.comunicado.application.command.ScheduleAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementMustBeInTheFutureException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementPermissionDeniedException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementDestinationRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.module.tag.application.dto.TagLinkDestinationCommand;
import com.portal.conecta.comunicados.module.tag.application.usecase.AutoLinkTagsByDestinationUseCase;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import com.portal.conecta.comunicados.shared.exception.UnauthorizedUserException;

import lombok.RequiredArgsConstructor;

/**
 * Criação + agendamento atômico (#108): cria o comunicado já {@code SCHEDULED}, com destinos e
 * histórico (CREATION + SCHEDULED), numa única transação. {@code scheduledFor} precisa ser futuro.
 */
@Service
@RequiredArgsConstructor
public class ScheduleAnnouncementUseCase {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementDestinationRepository announcementDestinationRepository;
    private final AnnouncementHistoryRepository announcementHistoryRepository;
    private final RequestContextProvider requestContextProvider;
    private final AnnouncementPermissionValidator permissionValidator;
    private final AutoLinkTagsByDestinationUseCase autoLinkTagsUseCase;

    @Transactional
    public Announcement execute(ScheduleAnnouncementCommand command) {
        RequestContext context = requestContextProvider.getRequestContext();

        validateAuthenticatedUser(context);
        validateScheduledFor(command);
        validatePermission(command, context);

        Instant now = Instant.now();

        Announcement announcement = announcementRepository.save(command.toEntity(now));
        List<AnnouncementDestination> destinations = announcementDestinationRepository.saveAll(command.toDestinations(announcement));
        autoLinkTagsUseCase.execute(announcement, toTagCommands(destinations));

        announcementHistoryRepository.save(command.toCreationHistory(announcement, now));
        announcementHistoryRepository.save(command.toScheduleHistory(announcement, now));

        return announcement;
    }

    private void validateAuthenticatedUser(RequestContext context) {
        if (context == null || context.userId() == null || context.userType() == null) {
            throw new UnauthorizedUserException();
        }
    }

    private void validateScheduledFor(ScheduleAnnouncementCommand command) {
        if (command.scheduledFor() == null || !command.scheduledFor().isAfter(Instant.now())) {
            throw new AnnouncementMustBeInTheFutureException();
        }
    }

    private void validatePermission(ScheduleAnnouncementCommand command, RequestContext context) {
        if (!permissionValidator.canCreateForDestinations(context, command.destinations())) {
            throw new AnnouncementPermissionDeniedException();
        }
    }

    private List<TagLinkDestinationCommand> toTagCommands(List<AnnouncementDestination> destinations) {
        return destinations.stream()
                .map(d -> new TagLinkDestinationCommand(d.getType(), d.getReferenceId()))
                .toList();
    }
}
