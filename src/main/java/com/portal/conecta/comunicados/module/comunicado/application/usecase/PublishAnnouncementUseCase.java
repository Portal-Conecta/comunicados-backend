package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portal.conecta.comunicados.module.comunicado.application.command.PublishAnnouncementCommand;
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
 * Criação + publicação atômica (#107): cria o comunicado já {@code PUBLISHED}, com destinos e
 * histórico (CREATION + PUBLICATION), numa única transação. O autor/publicador é o usuário
 * autenticado.
 */
@Service
@RequiredArgsConstructor
public class PublishAnnouncementUseCase {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementDestinationRepository announcementDestinationRepository;
    private final AnnouncementHistoryRepository announcementHistoryRepository;
    private final RequestContextProvider requestContextProvider;
    private final AnnouncementPermissionValidator permissionValidator;
    private final AutoLinkTagsByDestinationUseCase autoLinkTagsUseCase;

    @Transactional
    public Announcement execute(PublishAnnouncementCommand command) {
        RequestContext context = requestContextProvider.getRequestContext();

        validateAuthenticatedUser(context);
        validatePermission(command, context);

        Instant now = Instant.now();

        Announcement announcement = announcementRepository.save(command.toEntity(now));
        List<AnnouncementDestination> destinations = announcementDestinationRepository.saveAll(command.toDestinations(announcement));

        List<UUID> explicitTagIds = command.tagIds() != null ? command.tagIds() : List.of();

        autoLinkTagsUseCase.execute(announcement, toTagCommands(destinations), explicitTagIds);

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
        if (!permissionValidator.canCreateForDestinations(context, command.destinations())) {
            throw new AnnouncementPermissionDeniedException("Usuário não tem permissão para publicar comunicados (Validar permissões).");
        }
    }

    private List<TagLinkDestinationCommand> toTagCommands(List<AnnouncementDestination> destinations) {
        return destinations.stream()
                .map(d -> new TagLinkDestinationCommand(d.getType(), d.getReferenceId()))
                .toList();
    }
}