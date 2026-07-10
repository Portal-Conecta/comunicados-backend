package com.portal.conecta.comunicados.module.comunicado.domain.port.presign;

import java.util.Map;

public record PresignedUpload(
        String url,
        Map<String, String> fields,
        String s3Key,
        String bucket
) {
}
