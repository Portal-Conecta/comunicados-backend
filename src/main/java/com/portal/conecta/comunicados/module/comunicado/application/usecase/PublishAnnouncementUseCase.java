package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.portal.conecta.comunicados.module.comunicado.application.command.PublishAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;

import lombok.RequiredArgsConstructor;

/**
 * Publicação legada via PATCH — transiciona comunicados {@link AnnouncementStatus#SCHEDULED} para
 * {@link AnnouncementStatus#PUBLISHED}. Será usado pelo job de agendamento e, após #107, coexistirá
 * com {@code POST /api/posts/publish} para criação + publicação atômica.
 */
@Service
@RequiredArgsConstructor
public class PublishAnnouncementUseCase {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementHistoryRepository announcementHistoryRepository;
    private final RequestContextProvider requestContextProvider;
    private final AnnouncementPermissionValidator permissionValidator;

    @Transactional
    public Announcement execute(PublishAnnouncementCommand command) {
        RequestContext context = requestContextProvider.getRequestContext();

        Announcement announcement = announcementRepository.findByIdAndRemovedAtIsNull(command.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comunicado não encontrado!"));

        validateCanPublish(announcement, context);

        Instant now = Instant.now();

        announcement.setStatus(AnnouncementStatus.PUBLISHED);
        announcement.setPublishedAt(now);
        announcement.setPublishedByUserId(context.userId());

        Announcement savedAnnouncement = announcementRepository.save(announcement);
        saveHistory(savedAnnouncement, context.userId(), now);

        return savedAnnouncement;
    }

    private void validateCanPublish(Announcement announcement, RequestContext context) {
        validateAuthenticatedUser(context);
        validateStatus(announcement);
        validateRequiredFields(announcement);
        validatePermission(announcement, context);
    }

    private void validateStatus(Announcement announcement) {
        if (announcement.getStatus() != AnnouncementStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente comunicados agendados podem ser publicados!");
        }
    }

    private void validateAuthenticatedUser(RequestContext context) {
        if (context == null || context.userId() == null || context.userType() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário autenticado é obrigatório!");
        }
    }

    private void validateRequiredFields(Announcement announcement) {
        if (isBlank(announcement.getTitle())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título é obrigatório para publicar o comunicado!");
        }

        if (isBlank(announcement.getDescription())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Descrição é obrigatória para publicar o comunicado!");
        }

        if (announcement.getDestinations() == null || announcement.getDestinations().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pelo menos um destino é obrigatório para publicar o comunicado!");
        }
    }

    private void validatePermission(Announcement announcement, RequestContext context) {
        if (!permissionValidator.canPublishOrSchedule(announcement, context)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário sem permissão para publicar este comunicado!");
        }
    }

    private void saveHistory(Announcement announcement, UUID publishedByUserId, Instant now) {
        AnnouncementHistory history = new AnnouncementHistory();

        history.setAnnouncement(announcement);
        history.setUserId(publishedByUserId);
        history.setAction(AnnouncementHistoryAction.PUBLICATION);
        history.setSnapshot(createSnapshot(announcement));
        history.setCreatedAt(now);

        announcementHistoryRepository.save(history);
    }

    private String createSnapshot(Announcement announcement) {
        return "Comunicado publicado: "
                + announcement.getTitle()
                + " | status: "
                + announcement.getStatus();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
