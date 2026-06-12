package com.portal.conecta.comunicados.module.comunicado.domain.validator;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
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

    public boolean canCreate(UserType userType) {
        if (userType == null) {
            return false;
        }
        return ALLOWED_TYPES.contains(userType);
    }

    /**
     * Permissão de criação (publicar/agendar) avaliada pelos destinos enviados no request (#107 / #108).
     * SENAI/WEG/ADMIN podem qualquer escopo; docente e representante só podem criar quando todos os
     * destinos estão dentro das turmas sob sua alçada (turma diretamente ou turma do aluno, no destino USER).
     */
    public boolean canCreateForDestinations(
            RequestContext context,
            List<CreateAnnouncementDestinationInput> destinations
    ) {
        if (context == null || context.userType() == null) {
            return false;
        }
        if (!canCreate(context.userType())) {
            return false;
        }
        if (canManageAnyScope(context.userType())) {
            return true;
        }
        if (context.userType() == UserType.TEACHER) {
            return allDestinationsWithinClasses(destinations, context, ClassRole.TEACHER);
        }
        if (context.userType() == UserType.REPRESENTATIVE) {
            return allDestinationsWithinClasses(destinations, context, ClassRole.REPRESENTATIVE);
        }
        return false;
    }

    private boolean canManageAnyScope(UserType userType) {
        return userType == UserType.SENAI
                || userType == UserType.WEG
                || userType == UserType.ADMIN;
    }

    private boolean allDestinationsWithinClasses(
            List<CreateAnnouncementDestinationInput> destinations,
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

        if (destinations == null || destinations.isEmpty()) {
            return false;
        }

        return destinations.stream()
                .allMatch(destination -> isDestinationWithinClasses(destination, allowedClassIds));
    }

    private boolean isDestinationWithinClasses(
            CreateAnnouncementDestinationInput destination,
            List<UUID> allowedClassIds
    ) {
        if (destination == null || destination.type() == null || destination.referenceId() == null) {
            return false;
        }

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
