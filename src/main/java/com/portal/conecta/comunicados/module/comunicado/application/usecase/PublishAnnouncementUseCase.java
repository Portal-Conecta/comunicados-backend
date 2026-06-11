package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import com.portal.conecta.comunicados.module.comunicado.application.command.PublishAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.portal.conecta.comunicados.shared.context.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PublishAnnouncementUseCase {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementHistoryRepository announcementHistoryRepository;
    private final RequestContextProvider requestContextProvider;

    public PublishAnnouncementUseCase(
            AnnouncementRepository announcementRepository,
            AnnouncementHistoryRepository announcementHistoryRepository,
            RequestContextProvider requestContextProvider
    ) {
        this.announcementRepository = announcementRepository;
        this.announcementHistoryRepository = announcementHistoryRepository;
        this.requestContextProvider = requestContextProvider;
    }

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
        validatePublisher(context);
        validateStatus(announcement);
        validateRequiredFields(announcement);
        validatePermission(announcement, context);
    }

    private void validatePublisher(RequestContext context) {
        if (context == null || context.userId() == null || context.userType() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário autenticado é obrigatório!");
        }
    }

    private void validateStatus(Announcement announcement) {
        if (announcement.getStatus() != AnnouncementStatus.DRAFT && announcement.getStatus() != AnnouncementStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente comunicados em rascunho ou agendados podem ser publicados!");
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
        if (canPublishAnyScope(context.userType())) {
            return;
        }

        if (context.userType() == UserType.STUDENT) {
            throw forbidden();
        }

        if (context.userType() == UserType.TEACHER && canPublishLinkedClasses(announcement, context, ClassRole.TEACHER)) {
            return;
        }

        if (context.userType() == UserType.REPRESENTATIVE && canPublishLinkedClasses(announcement, context, ClassRole.REPRESENTATIVE)) {
            return;
        }

        throw forbidden();
    }

    private boolean canPublishAnyScope(UserType userType) {
        return userType == UserType.SENAI
                || userType == UserType.WEG
                || userType == UserType.ADMIN;
    }

    private boolean canPublishLinkedClasses(Announcement announcement, RequestContext context, ClassRole requiredRole) {
        List<ContextClass> contextClasses = context.classes() == null ? List.of() : context.classes();

        List<UUID> allowedClassIds = contextClasses.stream()
                .filter(contextClass -> contextClass.role() == requiredRole)
                .map(ContextClass::classId)
                .toList();

        if (allowedClassIds.isEmpty()) {
            return false;
        }

        return announcement.getDestinations().stream()
                .allMatch(destination -> isAllowedClassDestination(destination, allowedClassIds));
    }

    private boolean isAllowedClassDestination(
            AnnouncementDestination destination,
            List<UUID> allowedClassIds
    ) {
        return destination.getType() == AnnouncementDestinationType.CLASS
                && destination.getReferenceId() != null
                && allowedClassIds.contains(destination.getReferenceId());
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário sem permissão para publicar este comunicado!");
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