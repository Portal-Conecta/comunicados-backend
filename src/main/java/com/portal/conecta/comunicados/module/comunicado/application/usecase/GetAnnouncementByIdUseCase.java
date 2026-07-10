package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import com.portal.conecta.comunicados.module.comunicado.application.query.GetAnnouncementByIdQuery;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.hub.HubCoursePort;
import com.portal.conecta.comunicados.module.comunicado.domain.port.hub.HubShiftPort;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.AnnouncementTagRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.shared.context.ContextClass;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetAnnouncementByIdUseCase {

    private final AnnouncementRepository announcementRepository;
    private final RequestContextProvider contextProvider;
    private final AnnouncementPermissionValidator permissionValidator;
    private final HubCoursePort hubCoursePort;
    private final HubShiftPort hubShiftPort;
    private final AnnouncementTagRepository announcementTagRepository;

    @Transactional(readOnly = true)
    public Announcement execute(GetAnnouncementByIdQuery query) {
        RequestContext context = contextProvider.getRequestContext();

        Announcement announcement = announcementRepository.findByIdAndRemovedAtIsNull(query.id())
                .orElseThrow(AnnouncementNotFoundException::new);

        if (!permissionValidator.canViewAll(context.userType())
                && !isVisibleTo(announcement, context)) {
            // Não vaza existência: fora do escopo responde como inexistente (404).
            throw new AnnouncementNotFoundException();
        }

        // OSIV desligado: inicializa as coleções do detalhe dentro da transação.
        announcement.getDestinations().size();
        announcement.getFiles().size();
        announcement.getTags().size();
        announcement.getMentions().size();

        return announcement;
    }

    private boolean isVisibleTo(Announcement announcement, RequestContext context) {
        List<UUID> classIds = classIds(context);
        List<UUID> courseIds = hubCoursePort.getCurrentUserCourseIds();
        UUID viewerUserId = context.userId();

        boolean destinationMatch = announcement.getDestinations()
                .stream()
                .anyMatch(destination -> matches(destination, classIds, courseIds, viewerUserId));

        if (!destinationMatch) {
            return false;
        }

        return matchesShiftRestriction(announcement.getId(), classIds);
    }

    private boolean matchesShiftRestriction(UUID announcementId, List<UUID> classIds) {
        List<String> requiredShiftCodes = announcementTagRepository.findActiveShiftHubEntityIdsByAnnouncementId(announcementId);
        if (requiredShiftCodes.isEmpty()) {
            return true;
        }

        List<String> viewerShiftCodes = hubShiftPort.getShiftCodesForClasses(classIds);
        return requiredShiftCodes.stream().anyMatch(viewerShiftCodes::contains);
    }

    private boolean matches(
            AnnouncementDestination destination,
            List<UUID> classIds,
            List<UUID> courseIds,
            UUID viewerUserId
    ) {
        AnnouncementDestinationType type = destination.getType();
        if (type == AnnouncementDestinationType.GENERAL) {
            return true;
        }
        if (type == AnnouncementDestinationType.CLASS) {
            return destination.getReferenceId() != null
                    && classIds.contains(destination.getReferenceId());
        }
        if (type == AnnouncementDestinationType.COURSE) {
            return destination.getReferenceId() != null
                    && courseIds.contains(destination.getReferenceId());
        }
        if (type == AnnouncementDestinationType.USER) {
            return viewerUserId != null
                    && viewerUserId.equals(destination.getReferenceId());
        }
        return false;
    }

    private List<UUID> classIds(RequestContext context) {
        if (context.classes() == null) {
            return List.of();
        }
        return context.classes()
                .stream()
                .map(ContextClass::classId)
                .toList();
    }
}
