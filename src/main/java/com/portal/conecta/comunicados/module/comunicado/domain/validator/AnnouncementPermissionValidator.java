package com.portal.conecta.comunicados.module.comunicado.domain.validator;

import com.portal.conecta.comunicados.shared.context.UserType;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

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
            UserType.ADMIN
    );

    public boolean canCreate(UserType userType) {
        if(userType == null) {
            return false;
        }
        return ALLOWED_TYPES.contains(userType);
    }

    public boolean canUpdate(UserType userType) {
        if(userType == null) {
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
}