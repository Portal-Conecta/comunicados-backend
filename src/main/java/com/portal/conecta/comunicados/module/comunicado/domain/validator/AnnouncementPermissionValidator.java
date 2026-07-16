package com.portal.conecta.comunicados.module.comunicado.domain.validator;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.ShiftCode;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.hub.HubStudent;
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
        return userType != null && PRIVILEGED.contains(userType);
    }

    /**
     * Valida destinos + audiência secundária (roles/turnos) para publish/schedule.
     * Gestores: livres. TEACHER/REPRESENTATIVE: escopo de turma/alunos; sem roles/shiftCodes.
     */
    public boolean canCreateAnnouncement(
            RequestContext context,
            List<CreateAnnouncementDestinationInput> destinations,
            List<UserType> roles,
            List<ShiftCode> shiftCodes
    ) {
        if (!canCreateForDestinations(context, destinations)) {
            return false;
        }
        if (context.userType() != null && SCOPED.contains(context.userType())) {
            if (roles != null && !roles.isEmpty()) {
                return false;
            }
            if (shiftCodes != null && !shiftCodes.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean canCreateForDestinations(RequestContext context, List<CreateAnnouncementDestinationInput> destinations) {
        if (context == null || context.userType() == null) {
            return false;
        }

        return switch (context.userType()) {
            case ADMIN, SENAI, WEG -> true;
            case TEACHER -> allTeacherDestinationsInScope(destinations, context);
            case REPRESENTATIVE -> allRepresentativeDestinationsInScope(destinations, context);
            default -> false;
        };
    }

    public boolean canDelete(UserType userType, UUID userId, Announcement announcement) {
        return canModify(userType, userId, announcement);
    }

    /**
     * TEACHER: CLASS das turmas em que é TEACHER, ou USER (aluno/representante) dessas turmas.
     * Rejeita GENERAL/COURSE.
     */
    private boolean allTeacherDestinationsInScope(
            List<CreateAnnouncementDestinationInput> destinations,
            RequestContext context
    ) {
        if (destinations == null || destinations.isEmpty()) {
            return false;
        }

        List<UUID> allowedClassIds = classIdsWithRole(context, ClassRole.TEACHER);
        if (allowedClassIds.isEmpty()) {
            return false;
        }

        return destinations.stream().allMatch(d -> isTeacherDestinationAllowed(d, allowedClassIds));
    }

    private boolean isTeacherDestinationAllowed(
            CreateAnnouncementDestinationInput destination,
            List<UUID> allowedClassIds
    ) {
        if (destination == null || destination.type() == null || destination.referenceId() == null) {
            return false;
        }
        return switch (destination.type()) {
            case CLASS -> allowedClassIds.contains(destination.referenceId());
            case USER -> isAudienceUserInClasses(destination.referenceId(), allowedClassIds);
            case GENERAL, COURSE -> false;
        };
    }

    /**
     * REPRESENTATIVE: apenas USER de alunos/representantes da(s) turma(s) em que é REPRESENTATIVE.
     * Sem CLASS/GENERAL/COURSE (alinha ao front: só alunos da própria turma).
     */
    private boolean allRepresentativeDestinationsInScope(
            List<CreateAnnouncementDestinationInput> destinations,
            RequestContext context
    ) {
        if (destinations == null || destinations.isEmpty()) {
            return false;
        }

        List<UUID> allowedClassIds = classIdsWithRole(context, ClassRole.REPRESENTATIVE);
        if (allowedClassIds.isEmpty()) {
            return false;
        }

        return destinations.stream().allMatch(d -> isRepresentativeDestinationAllowed(d, allowedClassIds));
    }

    private boolean isRepresentativeDestinationAllowed(
            CreateAnnouncementDestinationInput destination,
            List<UUID> allowedClassIds
    ) {
        if (destination == null || destination.type() == null || destination.referenceId() == null) {
            return false;
        }
        return switch (destination.type()) {
            case USER -> isAudienceUserInClasses(destination.referenceId(), allowedClassIds);
            case CLASS, GENERAL, COURSE -> false;
        };
    }

    private List<UUID> classIdsWithRole(RequestContext context, ClassRole requiredRole) {
        if (context.classes() == null) {
            return List.of();
        }
        return context.classes().stream()
                .filter(c -> c.role() == requiredRole)
                .map(ContextClass::classId)
                .toList();
    }

    /**
     * Aluno (STUDENT) ou representante da turma: lista de audiência no Hub, com fallback
     * {@code getClassIdForUser} se o Hub mapear o usuário a uma das turmas permitidas.
     */
    private boolean isAudienceUserInClasses(UUID userId, Collection<UUID> allowedClassIds) {
        if (userId == null || allowedClassIds == null || allowedClassIds.isEmpty()) {
            return false;
        }

        for (UUID classId : allowedClassIds) {
            List<HubStudent> audience = hubClassPort.findStudentsByClassId(classId);
            if (audience != null && audience.stream().anyMatch(s -> Objects.equals(userId, s.id()))) {
                return true;
            }
        }

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

    public boolean canReschedule(Announcement announcement, RequestContext context) {
        if (context == null || context.userType() == null) {
            return false;
        }
        if (PRIVILEGED.contains(context.userType())) {
            return true;
        }
        return announcement.getCreatedByUserId().equals(context.userId());
    }
}
