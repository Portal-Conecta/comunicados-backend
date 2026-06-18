package com.portal.conecta.comunicados.module.comunicado.domain.validator;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

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

    private static final EnumSet<UserType> PRIVILEGED = EnumSet.of(
            UserType.ADMIN, UserType.SENAI, UserType.WEG
    );

    private static final EnumSet<UserType> SCOPED = EnumSet.of(
            UserType.TEACHER, UserType.REPRESENTATIVE
    );

    private static final EnumSet<UserType> CREATOR_TEACHER_OR_REPRESENTATIVE = EnumSet.of(
            UserType.TEACHER, UserType.REPRESENTATIVE
    );

    private static final EnumSet<UserType> PIN_USER_TYPE = EnumSet.of(
            UserType.ADMIN,
            UserType.SENAI,
            UserType.WEG
    );

    public boolean canCreate(UserType userType) {
        return userType != null && (PRIVILEGED.contains(userType) || SCOPED.contains(userType));
    }

    public boolean canPin(UserType userType) {
        if (userType == null) {
            return false;
        }
        return PIN_USER_TYPE.contains(userType);
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

    public boolean canDelete(UserType userType, UUID userId, Announcement announcement) {
        return canModify(userType, userId, announcement);
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
        return canModify(userType, userId, announcement);
    }

    /**
     * Regra única de edição/remoção (#125/#126): a permissão depende do perfil de quem
     * <b>publicou</b> o comunicado ({@code createdByUserType}), não da origem institucional.
     *
     * <ul>
     *   <li>Autor SENAI/WEG: somente o próprio criador ou ADMIN.</li>
     *   <li>Autor TEACHER/REPRESENTATIVE: qualquer SENAI, WEG ou ADMIN (e o próprio autor).</li>
     * </ul>
     *
     * O perfil do autor é persistido na criação para não depender do Hub a cada operação;
     * quando ausente (comunicados legados), trata-se de forma conservadora (nega não-donos).
     */
    private boolean canModify(UserType userType, UUID userId, Announcement announcement) {
        if (userType == null || userId == null || announcement == null) {
            return false;
        }

        if (announcement.getStatus() == AnnouncementStatus.REMOVED || announcement.getRemovedAt() != null) {
            return false;
        }

        if (userType == UserType.ADMIN) {
            return true;
        }

        boolean isOwner = userId.equals(announcement.getCreatedByUserId());
        UserType creatorType = announcement.getCreatedByUserType();

        return switch (userType) {
            case TEACHER, REPRESENTATIVE -> isOwner;
            case SENAI, WEG -> isOwner || (creatorType != null && CREATOR_TEACHER_OR_REPRESENTATIVE.contains(creatorType));
            default -> false;
        };
    }
}
