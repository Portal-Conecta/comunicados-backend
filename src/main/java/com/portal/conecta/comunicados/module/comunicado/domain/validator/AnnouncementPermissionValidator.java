package com.portal.conecta.comunicados.module.comunicado.domain.validator;

import java.util.EnumSet;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
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

    public boolean canUpdate(UserType userType) {
        if (userType == null) {
            return false;
        }
        return ALLOWED_TYPES.contains(userType);
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
