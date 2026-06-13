package com.portal.conecta.comunicados.module.comunicado.domain.validator;

import java.util.EnumSet;
import java.util.UUID;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.shared.context.ClassRole;
import com.portal.conecta.comunicados.shared.context.ContextClass;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.UserType;

@Component
public class AnnouncementPermissionValidator {

    private static final EnumSet<UserType> ALLOWED_TYPES = EnumSet.of(
            UserType.REPRESENTATIVE,
            UserType.TEACHER,
            UserType.ADMIN,
            UserType.SENAI,
            UserType.WEG
    );

    private static final EnumSet<UserType> VIEW_ALL_TYPES = EnumSet.of(
            UserType.SENAI,
            UserType.WEG,
            UserType.ADMIN,
            UserType.TEACHER,
            UserType.REPRESENTATIVE
     );
  
    private static final EnumSet<UserType> CREATOR_TEACHER_OR_REPRESENTATIVE = EnumSet.of(
            UserType.TEACHER,
            UserType.REPRESENTATIVE
    );

    private static final EnumSet<UserType> PIN_USER_TYPE = EnumSet.of(
            UserType.ADMIN,
            UserType.SENAI,
            UserType.WEG
    );

    public boolean canCreate(UserType userType) {
        if (userType == null) {
            return false;
        }
        return ALLOWED_TYPES.contains(userType);
    }

    public boolean canPin(UserType userType) {
        if (userType == null) {
            return false;
        }
        return PIN_USER_TYPE.contains(userType);
    }

    /**
     * Permissão compartilhada por publicar, agendar e futuros fluxos unificados (#107 / #108).
     */
    public boolean canPublishOrSchedule(Announcement announcement, RequestContext context) {
        if (context == null || context.userType() == null || announcement == null) {
            return false;
        }
        if (!canCreate(context.userType())) {
            return false;
        }
        if (canManageAnyScope(context.userType())) {
            return true;
        }
        if (context.userType() == UserType.TEACHER) {
            return canManageLinkedClasses(announcement, context, ClassRole.TEACHER);
        }
        if (context.userType() == UserType.REPRESENTATIVE) {
            return canManageLinkedClasses(announcement, context, ClassRole.REPRESENTATIVE);
        }
        return false;
    }

    private boolean canManageAnyScope(UserType userType) {
        return userType == UserType.SENAI
                || userType == UserType.WEG
                || userType == UserType.ADMIN;
    }

    private boolean canManageLinkedClasses(
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

        if (announcement.getDestinations() == null || announcement.getDestinations().isEmpty()) {
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

    public boolean canUpdate(UserType userType) {
        if (userType == null) {
            return false;
        }
        return ALLOWED_TYPES.contains(userType);
    }

    public boolean canViewAll(UserType userType) {
        if(userType == null) {
            return false;
        }
        return VIEW_ALL_TYPES.contains(userType);
    }
  
    public boolean canDelete(UserType userType, UUID userId, Announcement announcement, UserType creatorType) {
        if (userType == null || userId == null || announcement == null) {
            return false;
        }
        if (announcement.getStatus() == AnnouncementStatus.REMOVED) {
            return false;
        }
        if (userType == UserType.ADMIN) {
            return true;
        }
        if (userId.equals(announcement.getCreatedByUserId())) {
            return true;
        }
        if (userType == UserType.TEACHER || userType == UserType.REPRESENTATIVE) {
            return false;
        }
        if (creatorType == null) {
            return false;
        }
        if (userType == UserType.SENAI || userType == UserType.WEG) {
            return CREATOR_TEACHER_OR_REPRESENTATIVE.contains(creatorType);
        }
        return false;
    }
}
