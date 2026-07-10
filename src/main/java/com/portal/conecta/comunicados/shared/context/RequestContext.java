package com.portal.conecta.comunicados.shared.context;

import java.util.List;
import java.util.UUID;

public record RequestContext(
        UUID userId,
        UserType userType,
        List<ContextClass> classes,
        Integer permissionVersion
) {

    public RequestContext(UUID userId, UserType userType, List<ContextClass> classes) {
        this(userId, userType, classes, null);
    }

}