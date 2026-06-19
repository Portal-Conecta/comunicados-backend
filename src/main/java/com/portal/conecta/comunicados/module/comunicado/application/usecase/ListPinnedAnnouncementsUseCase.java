package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.hub.HubCoursePort;
import com.portal.conecta.comunicados.module.comunicado.domain.specification.AnnouncementSpecifications;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.shared.context.ContextClass;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ListPinnedAnnouncementsUseCase {

    private static final Sort PINNED_ORDER = Sort.by(Sort.Order.asc("pinnedOrder"));

    private final AnnouncementRepository announcementRepository;
    private final RequestContextProvider contextProvider;
    private final AnnouncementPermissionValidator permissionValidator;
    private final HubCoursePort hubCoursePort;

    @Transactional(readOnly = true)
    public List<Announcement> execute() {
        RequestContext context = contextProvider.getRequestContext();

        Specification<Announcement> spec = AnnouncementSpecifications.notRemoved()
                .and(AnnouncementSpecifications.isPublished())
                .and(AnnouncementSpecifications.isPinned());

        if (!permissionValidator.canViewAll(context.userType())) {
            spec = spec.and(AnnouncementSpecifications.visibleTo(
                    classIds(context),
                    hubCoursePort.getCurrentUserCourseIds(),
                    context.userId()
            ));
        }

        return announcementRepository.findAll(spec, PINNED_ORDER);
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
