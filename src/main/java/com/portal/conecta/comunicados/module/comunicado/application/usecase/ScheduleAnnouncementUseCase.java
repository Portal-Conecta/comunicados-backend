package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.portal.conecta.comunicados.module.comunicado.application.command.ScheduleAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementMustBeInTheFutureException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.shared.context.ClassRole;
import com.portal.conecta.comunicados.shared.context.ContextClass;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import com.portal.conecta.comunicados.shared.context.UserType;
import com.portal.conecta.comunicados.shared.exception.UnauthorizedUserException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduleAnnouncementUseCase {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementHistoryRepository announcementHistoryRepository;
    private final RequestContextProvider requestContextProvider;


    @Transactional
    public Announcement execute(ScheduleAnnouncementCommand command) {
        RequestContext context = requestContextProvider.getRequestContext();

        Announcement announcement = announcementRepository.findByIdAndRemovedAtIsNull(command.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comunicado não encontrado!"));

        validateCanSchedule(announcement, command, context);

        Instant now = Instant.now();

        Announcement scheduledAnnouncement = command.toEntity(announcement, now);
        Announcement savedAnnouncement = announcementRepository.save(scheduledAnnouncement);

        AnnouncementHistory history = command.toHistory(savedAnnouncement, context.userId(), now);
        announcementHistoryRepository.save(history);

        return savedAnnouncement;
    }

    private void validateCanSchedule(
            Announcement announcement,
            ScheduleAnnouncementCommand command,
            RequestContext context
    ) {
        validateAuthenticatedUser(context);
        validateScheduledFor(command, Instant.now());
        validateStatus(announcement);
        validateRequiredFields(announcement);
        validatePermission(announcement, context);
    }

    private void validateAuthenticatedUser(RequestContext context) {
        if (context == null || context.userId() == null || context.userType() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário autenticado é obrigatório!");
        }
    }

    private void validateScheduledFor(ScheduleAnnouncementCommand command, Instant now) {
        if (command.data().scheduledFor() == null || !command.data().scheduledFor().isAfter(now)) {
            throw new AnnouncementMustBeInTheFutureException();
        }
    }

    private void validateStatus(Announcement announcement) {
        if (announcement.getStatus() != AnnouncementStatus.DRAFT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Somente comunicados em rascunho podem ser agendados!"
            );
        }
    }

    private void validateRequiredFields(Announcement announcement) {
        if (isBlank(announcement.getTitle())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título é obrigatório para agendar o comunicado!");
        }

        if (isBlank(announcement.getDescription())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Descrição é obrigatória para agendar o comunicado!");
        }

        if (announcement.getDestinations() == null || announcement.getDestinations().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Pelo menos um destino é obrigatório para agendar o comunicado!"
            );
        }
    }

    private void validatePermission(Announcement announcement, RequestContext context) {
        if (canScheduleAnyScope(context.userType())) {
            return;
        }

        if (context.userType() == UserType.STUDENT) {
            throw forbidden();
        }

        if (context.userType() == UserType.TEACHER && canScheduleLinkedClasses(announcement, context, ClassRole.TEACHER)) {
            return;
        }

        if (context.userType() == UserType.REPRESENTATIVE
                && canScheduleLinkedClasses(announcement, context, ClassRole.REPRESENTATIVE)) {
            return;
        }

        throw forbidden();
    }

    private boolean canScheduleAnyScope(UserType userType) {
        return userType == UserType.SENAI
                || userType == UserType.WEG
                || userType == UserType.ADMIN;
    }

    private boolean canScheduleLinkedClasses(
            Announcement announcement,
            RequestContext context,
            ClassRole requiredRole
    ) {
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

    private UnauthorizedUserException forbidden() {
        return new UnauthorizedUserException();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
