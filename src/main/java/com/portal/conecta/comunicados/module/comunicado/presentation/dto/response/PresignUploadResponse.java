package com.portal.conecta.comunicados.module.comunicado.presentation.dto.response;

import java.util.Map;
import java.util.UUID;

public record PresignUploadResponse(

        UUID fileId,
        String uploadUrl,
        Map<String, String> fields

) {}
