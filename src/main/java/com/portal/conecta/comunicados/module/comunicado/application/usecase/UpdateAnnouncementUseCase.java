package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

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
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.AnnouncementTagRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.service.AnnouncementDescriptionNormalizer;
import com.portal.conecta.comunicados.module.comunicado.domain.service.NormalizedAnnouncementDescription;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.module.tag.application.dto.TagLinkDestinationCommand;
import com.portal.conecta.comunicados.module.tag.application.usecase.AutoLinkTagsByDestinationUseCase;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateAnnouncementUseCase {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementDestinationRepository destinationRepository;
    private final AnnouncementHistoryRepository historyRepository;
    private final AnnouncementTagRepository announcementTagRepository;
    private final RequestContextProvider contextProvider;
    private final AnnouncementPermissionValidator permissionValidator;
    private final AnnouncementDescriptionNormalizer descriptionNormalizer;
    private final AutoLinkTagsByDestinationUseCase autoLinkTagsUseCase;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Announcement execute(UpdateAnnouncementCommand command) {
        RequestContext context = contextProvider.getRequestContext();
        Announcement announcement = announcementRepository.findById(command.id())
                .orElseThrow(AnnouncementNotFoundException::new);

        if (announcement.getRemovedAt() != null || announcement.getStatus() == AnnouncementStatus.REMOVED) {
            throw new AnnouncementConflictException("Comunicados removidos não podem ser editados.");
        }

        if (!permissionValidator.canUpdate(context.userType(), context.userId(), announcement)) {
            throw new AnnouncementPermissionDeniedException("Usuário não tem permissão para editar este comunicado.");
        }

        Instant now = Instant.now();
        List<AnnouncementDestination> currentDestinations = destinationRepository.findByAnnouncementId(command.id());
        String snapshot = announcement.getStatus() == AnnouncementStatus.PUBLISHED
                ? command.toSnapshot(announcement, currentDestinations, objectMapper)
                : null;

        String sanitizedHtml = null;
        String descriptionPlain = null;
        if (command.data().description() != null) {
            NormalizedAnnouncementDescription description =
                    descriptionNormalizer.normalize(command.data().description());
            sanitizedHtml = description.html();
            descriptionPlain = description.plain();
        }

        Announcement updated = command.toEntity(announcement, now, sanitizedHtml, descriptionPlain);
        updated = announcementRepository.save(updated);

        // Edição parcial: só substitui os destinos quando o cliente os enviou no body;
        // ausentes (null) preservam os destinos atuais.
        if (command.data().destinations() != null) {
            destinationRepository.deleteByAnnouncementId(updated.getId());
            announcementTagRepository.deleteByAnnouncementId(updated.getId());

            List<AnnouncementDestination> destinations = command.toDestinations(updated);
            destinationRepository.saveAll(destinations);
            updated.setDestinations(destinations);

            autoLinkTagsUseCase.execute(updated, toTagCommands(destinations));
        }

        historyRepository.save(command.toEditHistory(updated, now, snapshot));

        return updated;
    }

    private List<TagLinkDestinationCommand> toTagCommands(List<AnnouncementDestination> destinations) {
        return destinations.stream()
                .map(d -> new TagLinkDestinationCommand(d.getType(), d.getReferenceId()))
                .toList();
    }

}