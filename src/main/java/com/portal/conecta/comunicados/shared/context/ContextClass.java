package com.portal.conecta.comunicados.shared.context;

import java.util.UUID;

public record ContextClass(
        UUID classId,
        ClassRole role
) {
}
