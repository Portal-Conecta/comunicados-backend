package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import com.portal.conecta.comunicados.module.comunicado.application.query.ListAnnouncementsQuery;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ListAnnouncementsUseCase {

    private final AnnouncementRepository announcementRepository;
    private final RequestContextProvider contextProvider;
    private final AnnouncementPermissionValidator permissionValidator;
    private final HubCoursePort hubCoursePort;
    private final HubUserPort hubUserPort;

    @Transactional(readOnly = true)
    public Page<Announcement> execute(ListAnnouncementsQuery query) {
        RequestContext context = contextProvider.getRequestContext();
        PostFilterRequest filter = query.filter();

        Specification<Announcement> spec = AnnouncementSpecifications.notRemoved()
                .and(AnnouncementSpecifications.hasOrigin(filter.origin()))
                .and(AnnouncementSpecifications.publishedBetween(filter.publishedFrom(), filter.publishedTo()))
                .and(AnnouncementSpecifications.hasDestinationClass(filter.classId()));

        if (hasSearch(filter)) {
            String searchTerm = filter.search().trim();
            List<UUID> matchingUserIds = hubUserPort.findUserIdsByNameContaining(searchTerm, context);
            spec = spec.and(AnnouncementSpecifications.matchesSearch(searchTerm, matchingUserIds));
        }

        if (!permissionValidator.canViewAll(context.userType())) {
            spec = spec.and(AnnouncementSpecifications.visibleTo(
                    classIds(context),
                    hubCoursePort.getCurrentUserCourseIds(),
                    context.userId()
            ));
        }

        return announcementRepository.findAll(spec, query.toPageRequest());
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
