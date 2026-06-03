package com.portal.conecta.comunicados.shared.context;

import java.util.List;
import java.util.UUID;

public record RequestContext(
    UUID userId,
    UserType userType,
    List<ContextClass> classes
) { }
