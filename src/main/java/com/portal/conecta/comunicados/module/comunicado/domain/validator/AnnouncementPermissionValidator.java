package com.portal.conecta.comunicados.module.comunicado.domain.validator;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubClassPort;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementDestinationInput;
import com.portal.conecta.comunicados.shared.context.ClassRole;
import com.portal.conecta.comunicados.shared.context.ContextClass;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.UserType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AnnouncementPermissionValidator {

    private final HubClassPort hubClassPort;

    private static final EnumSet<UserType> PRIVILEGED = EnumSet.of(
            UserType.ADMIN, UserType.SENAI, UserType.WEG
    );

    private static final EnumSet<UserType> SCOPED = EnumSet.of(
            UserType.TEACHER, UserType.REPRESENTATIVE
    );

    private static final EnumSet<UserType> CREATOR_TEACHER_OR_REPRESENTATIVE = EnumSet.of(
            UserType.TEACHER, UserType.REPRESENTATIVE
    );

    public boolean canCreate(UserType userType) {
        return userType != null && (PRIVILEGED.contains(userType) || SCOPED.contains(userType));
    }

    public boolean canUpdate(UserType userType) {
        return canCreate(userType);
    }

    public boolean canViewAll(UserType userType) {
        return canCreate(userType);
    }

    public boolean canCreateForDestinations(RequestContext context, List<CreateAnnouncementDestinationInput> destinations) {
        if (context == null || context.userType() == null) return false;

        return switch (context.userType()) {
            case ADMIN, SENAI, WEG -> true;
            case TEACHER -> allDestinationsWithinClasses(destinations, context, ClassRole.TEACHER);
            case REPRESENTATIVE -> allDestinationsWithinClasses(destinations, context, ClassRole.REPRESENTATIVE);
            default -> false;
        };
    }

    public boolean canDelete(UserType userType, UUID userId, Announcement announcement, UserType creatorType) {
        if (userType == null || userId == null || announcement == null) return false;
        if (announcement.getStatus() == AnnouncementStatus.REMOVED) return false;
        if (userType == UserType.ADMIN) return true;

        boolean isOwner = userId.equals(announcement.getCreatedByUserId());

        return switch (userType) {
            case TEACHER, REPRESENTATIVE -> isOwner;
            case SENAI, WEG -> isOwner || (creatorType != null && CREATOR_TEACHER_OR_REPRESENTATIVE.contains(creatorType));
            default -> false;
        };
    }

    private boolean allDestinationsWithinClasses(
            List<CreateAnnouncementDestinationInput> destinations,
            RequestContext context,
            ClassRole requiredRole
    ) {
        if (destinations == null || destinations.isEmpty()) return false;

        List<UUID> allowedClassIds = context.classes() == null ? List.of() : context.classes().stream()
                .filter(c -> c.role() == requiredRole)
                .map(ContextClass::classId)
                .toList();

        if (allowedClassIds.isEmpty()) return false;

        return destinations.stream().allMatch(d -> isDestinationWithinClasses(d, allowedClassIds));
    }

    private boolean isDestinationWithinClasses(CreateAnnouncementDestinationInput destination, List<UUID> allowedClassIds) {
        if (destination == null || destination.type() == null || destination.referenceId() == null) return false;

        return switch (destination.type()) {
            case CLASS -> allowedClassIds.contains(destination.referenceId());
            case USER -> isUserWithinClasses(destination.referenceId(), allowedClassIds);
            case GENERAL, COURSE -> false;
        };
    }

    private boolean isUserWithinClasses(UUID userId, List<UUID> allowedClassIds) {
        UUID classId = hubClassPort.getClassIdForUser(userId);
        return classId != null && allowedClassIds.contains(classId);
    }

    public boolean canUpdate(UserType userType, UUID userId, Announcement announcement) {
        if (userType == null || userId == null || announcement == null) {
            return false;
        }

        if (announcement.getStatus() == AnnouncementStatus.REMOVED || announcement.getRemovedAt() != null) {
            return false;
        }

        if (userType == UserType.ADMIN || userType == UserType.SENAI || userType == UserType.WEG) {
            return true;
        }

        if (userType == UserType.TEACHER || userType == UserType.REPRESENTATIVE) {
            return userId.equals(announcement.getCreatedByUserId());
        }

        return false;
    }

    public boolean canReschedule(Announcement announcement, RequestContext context) {
        if (context == null || context.userType() == null) return false;
        if (PRIVILEGED.contains(context.userType())) return true;
        return announcement.getCreatedByUserId().equals(context.userId());
    }
}
