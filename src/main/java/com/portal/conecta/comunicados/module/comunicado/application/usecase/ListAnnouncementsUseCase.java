package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import com.portal.conecta.comunicados.module.comunicado.application.query.ListAnnouncementsQuery;
import com.portal.conecta.comunicados.module.comunicado.application.query.ListAnnouncementsResult;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementPermissionDeniedException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.hub.HubCoursePort;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubUserPort;
import com.portal.conecta.comunicados.module.comunicado.domain.specification.AnnouncementSpecifications;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.PostFilterRequest;
import com.portal.conecta.comunicados.shared.context.ContextClass;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ListAnnouncementsUseCase {

    private static final Sort PINNED_ORDER = Sort.by(Sort.Order.asc("pinnedOrder"));

    private final AnnouncementRepository announcementRepository;
    private final RequestContextProvider contextProvider;
    private final AnnouncementPermissionValidator permissionValidator;
    private final HubCoursePort hubCoursePort;
    private final HubUserPort hubUserPort;

    @Transactional(readOnly = true)
    public ListAnnouncementsResult execute(ListAnnouncementsQuery query) {
        RequestContext context = contextProvider.getRequestContext();
        Specification<Announcement> baseSpec = buildBaseSpecification(context, query.filter());

        List<Announcement> pinned = announcementRepository.findAll(
                baseSpec.and(AnnouncementSpecifications.isPinned()),
                PINNED_ORDER
        );

        Page<Announcement> items = announcementRepository.findAll(
                baseSpec.and(AnnouncementSpecifications.isNotPinned()),
                query.toPageRequest()
        );

        return new ListAnnouncementsResult(pinned, items);
    }

    private Specification<Announcement> buildBaseSpecification(RequestContext context, PostFilterRequest filter) {
        boolean mineRequested = filter.mineRequested();

        if (mineRequested && !permissionValidator.canCreate(context.userType())) {
            throw new AnnouncementPermissionDeniedException(
                    "Usuário não tem permissão para listar seus comunicados.");
        }

        Specification<Announcement> spec = AnnouncementSpecifications.notRemoved()
                .and(AnnouncementSpecifications.hasOrigin(filter.origin()))
                .and(AnnouncementSpecifications.publishedBetween(filter.publishedFrom(), filter.publishedTo()))
                .and(AnnouncementSpecifications.hasDestinationClass(filter.classId()));

        if (mineRequested) {
            // "Meus Comunicados": escopo por autoria. Inclui os agendados (SCHEDULED), então não
            // exige publicação; e ser autor dispensa a regra de visibilidade (visibleTo).
            spec = spec.and(AnnouncementSpecifications.createdBy(context.userId()));
        } else {
            spec = spec.and(AnnouncementSpecifications.isPublished());
        }

        if (hasSearch(filter)) {
            String searchTerm = filter.search().trim();
            List<UUID> matchingUserIds = hubUserPort.findUserIdsByNameContaining(searchTerm, context);
            spec = spec.and(AnnouncementSpecifications.matchesSearch(searchTerm, matchingUserIds));
        }

        List<UUID> tagIds = filter.resolvedTagIds();
        if (!tagIds.isEmpty()) {
            spec = spec.and(AnnouncementSpecifications.hasAnyTag(tagIds));
        }

        if (!mineRequested && !permissionValidator.canViewAll(context.userType())) {
            spec = spec.and(AnnouncementSpecifications.visibleTo(
                    classIds(context),
                    hubCoursePort.getCurrentUserCourseIds(),
                    context.userId()
            ));
        }

        return spec;
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

    private boolean hasSearch(PostFilterRequest filter) {
        return filter.search() != null && !filter.search().isBlank();
    }
}
