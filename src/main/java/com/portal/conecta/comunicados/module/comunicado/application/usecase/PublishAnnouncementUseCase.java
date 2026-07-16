package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portal.conecta.comunicados.module.comunicado.application.command.PublishAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.application.service.AnnouncementNotificationDispatcher;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.ShiftCode;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementPermissionDeniedException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementDestinationRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.service.AnnouncementDescriptionNormalizer;
import com.portal.conecta.comunicados.module.comunicado.domain.service.NormalizedAnnouncementDescription;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.module.tag.application.dto.TagLinkDestinationCommand;
import com.portal.conecta.comunicados.module.tag.application.usecase.AutoLinkTagsByDestinationUseCase;
import com.portal.conecta.comunicados.module.tag.application.usecase.LinkTagsByCodeUseCase;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import com.portal.conecta.comunicados.shared.context.UserType;
import com.portal.conecta.comunicados.shared.exception.UnauthorizedUserException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Criação + publicação atômica (#107): cria o comunicado já {@code PUBLISHED}, com destinos e
 * histórico (CREATION + PUBLICATION), numa única transação. O autor/publicador é o usuário
 * autenticado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublishAnnouncementUseCase {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementDestinationRepository announcementDestinationRepository;
    private final AnnouncementHistoryRepository announcementHistoryRepository;
    private final RequestContextProvider requestContextProvider;
    private final AnnouncementPermissionValidator permissionValidator;
    private final AnnouncementDescriptionNormalizer descriptionNormalizer;
    private final AutoLinkTagsByDestinationUseCase autoLinkTagsUseCase;
    private final LinkTagsByCodeUseCase linkTagsByCodeUseCase;
    private final AnnouncementNotificationDispatcher notificationDispatcher;

    @Transactional
    public Announcement execute(PublishAnnouncementCommand command) {
        RequestContext context = requestContextProvider.getRequestContext();

        validateAuthenticatedUser(context);
        validatePermission(command, context);

        Instant now = Instant.now();
        NormalizedAnnouncementDescription description = descriptionNormalizer.normalize(command.description());

        Announcement announcement = announcementRepository.save(
                command.toEntity(now, description.html(), description.plain()));
        List<AnnouncementDestination> destinations = announcementDestinationRepository.saveAll(command.toDestinations(announcement));
        autoLinkTagsUseCase.execute(announcement, toTagCommands(destinations), command.tagIds());
        linkTagsByCodeUseCase.execute(announcement, TagEntityType.SHIFT, command.shiftCodes().stream().map(ShiftCode::name).toList());
        linkTagsByCodeUseCase.execute(announcement, TagEntityType.ROLE, command.roles().stream().map(UserType::name).toList());

        notificationDispatcher.dispatchAfterCommit(announcement, destinations, command.roles());

        announcementHistoryRepository.save(command.toCreationHistory(announcement, now));
        announcementHistoryRepository.save(command.toPublicationHistory(announcement, now));

        return announcement;
    }

    private void validateAuthenticatedUser(RequestContext context) {
        if (context == null || context.userId() == null || context.userType() == null) {
            throw new UnauthorizedUserException();
        }
    }

    private void validatePermission(PublishAnnouncementCommand command, RequestContext context) {
        if (!permissionValidator.canCreateAnnouncement(
                context, command.destinations(), command.roles(), command.shiftCodes())) {
            throw new AnnouncementPermissionDeniedException(
                    "Usuário não tem permissão para publicar com os destinos/papéis/turnos informados.");
        }
    }

    private List<TagLinkDestinationCommand> toTagCommands(List<AnnouncementDestination> destinations) {
        return destinations.stream()
                .map(d -> new TagLinkDestinationCommand(d.getType(), d.getReferenceId()))
                .toList();
    }
}
