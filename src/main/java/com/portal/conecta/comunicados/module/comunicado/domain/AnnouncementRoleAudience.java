package com.portal.conecta.comunicados.module.comunicado.domain;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.portal.conecta.comunicados.shared.context.UserType;

/**
 * Regras de audiência por papel: representante também é aluno para visibilidade e notificação.
 * Um post restrito a {@code STUDENT} deve aparecer (e notificar) para {@code REPRESENTATIVE}.
 */
public final class AnnouncementRoleAudience {

    private AnnouncementRoleAudience() {
    }

    /**
     * Códigos ROLE que o viewer satisfaz na listagem/detalhe.
     * Representante cobre o próprio papel e {@code STUDENT}.
     */
    public static List<String> viewerRoleCodes(UserType viewerType) {
        if (viewerType == null) {
            return List.of();
        }
        if (viewerType == UserType.REPRESENTATIVE) {
            return List.of(UserType.REPRESENTATIVE.name(), UserType.STUDENT.name());
        }
        return List.of(viewerType.name());
    }

    public static boolean matchesRestriction(UserType viewerType, Collection<String> requiredRoleCodes) {
        if (requiredRoleCodes == null || requiredRoleCodes.isEmpty()) {
            return true;
        }
        if (viewerType == null) {
            return false;
        }
        Set<String> viewerCodes = Set.copyOf(viewerRoleCodes(viewerType));
        return requiredRoleCodes.stream().anyMatch(viewerCodes::contains);
    }

    /**
     * Expande papéis de notificação: {@code STUDENT} implica {@code REPRESENTATIVE}.
     * A tag persistida no post permanece só o que o cliente enviou.
     */
    public static List<UserType> expandForNotification(List<UserType> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<UserType> expanded = new LinkedHashSet<>(roles);
        if (expanded.contains(UserType.STUDENT)) {
            expanded.add(UserType.REPRESENTATIVE);
        }
        return List.copyOf(expanded);
    }
}
