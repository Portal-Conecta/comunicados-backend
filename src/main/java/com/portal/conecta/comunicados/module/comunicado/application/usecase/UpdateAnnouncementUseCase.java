package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.conecta.comunicados.module.comunicado.application.command.UpdateAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementConflictException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementTag;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementDestinationRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.AnnouncementTagRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.service.AnnouncementDescriptionNormalizer;
import com.portal.conecta.comunicados.module.comunicado.domain.service.NormalizedAnnouncementDescription;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementDestinationInput;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementDestinationRequest;
import com.portal.conecta.comunicados.module.tag.application.dto.TagLinkDestinationCommand;
import com.portal.conecta.comunicados.module.tag.application.usecase.AutoLinkTagsByDestinationUseCase;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateAnnouncementUseCase {

    private static final Set<TagEntityType> DESTINATION_TAG_TYPES = EnumSet.of(
            TagEntityType.COURSE,
            TagEntityType.CLASS,
            TagEntityType.USER,
            TagEntityType.GENERAL
    );

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
            // Anti-vazamento: sem permissão responde como inexistente.
            throw new AnnouncementNotFoundException();
        }

        validateStatusTransition(announcement, command);
        validateDestinations(command, context);

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
        // ausentes (null) preservam os destinos atuais. Tags SHIFT/ROLE são preservadas.
        if (command.data().destinations() != null) {
            if (command.data().destinations().isEmpty()) {
                throw new AnnouncementConflictException("Informe ao menos um destino ao atualizar destinations.");
            }

            destinationRepository.deleteByAnnouncementId(updated.getId());
            deleteDestinationLinkedTags(updated.getId());

            List<AnnouncementDestination> destinations = command.toDestinations(updated);
            destinationRepository.saveAll(destinations);
            updated.setDestinations(destinations);

            autoLinkTagsUseCase.execute(updated, toTagCommands(destinations));
        }

        historyRepository.save(command.toEditHistory(updated, now, snapshot));

        return updated;
    }

    private void validateStatusTransition(Announcement announcement, UpdateAnnouncementCommand command) {
        AnnouncementStatus requested = command.data().status();
        if (requested == null) {
            return;
        }
        if (requested == AnnouncementStatus.REMOVED) {
            throw new AnnouncementConflictException(
                    "Remoção deve usar DELETE /api/posts/{id}; status REMOVED não é aceito no PUT.");
        }
        AnnouncementStatus current = announcement.getStatus();
        if (requested == current) {
            return;
        }
        if (current == AnnouncementStatus.SCHEDULED && requested == AnnouncementStatus.PUBLISHED) {
            return;
        }
        throw new AnnouncementConflictException(
                "Transição de status inválida: " + current + " → " + requested
                        + ". Use PATCH /schedule para reagendar ou DELETE para remover.");
    }

    private void validateDestinations(UpdateAnnouncementCommand command, RequestContext context) {
        if (command.data().destinations() == null) {
            return;
        }
        List<CreateAnnouncementDestinationInput> inputs = command.data().destinations().stream()
                .map(this::toInput)
                .toList();
        if (!permissionValidator.canCreateForDestinations(context, inputs)) {
            throw new AnnouncementConflictException(
                    "Usuário não tem permissão para os destinos informados.");
        }
    }

    private CreateAnnouncementDestinationInput toInput(CreateAnnouncementDestinationRequest request) {
        return new CreateAnnouncementDestinationInput(request.type(), request.referenceId());
    }

    private void deleteDestinationLinkedTags(java.util.UUID announcementId) {
        List<AnnouncementTag> tags = announcementTagRepository.findByAnnouncementId(announcementId);
        List<AnnouncementTag> toRemove = tags.stream()
                .filter(at -> at.getTag() != null && DESTINATION_TAG_TYPES.contains(at.getTag().getEntityType()))
                .toList();
        if (!toRemove.isEmpty()) {
            announcementTagRepository.deleteAll(toRemove);
        }
    }

    private List<TagLinkDestinationCommand> toTagCommands(List<AnnouncementDestination> destinations) {
        return destinations.stream()
                .map(d -> new TagLinkDestinationCommand(d.getType(), d.getReferenceId()))
                .toList();
    }
}
