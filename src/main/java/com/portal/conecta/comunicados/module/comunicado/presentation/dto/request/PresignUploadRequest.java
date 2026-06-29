package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PresignUploadRequest(

        @NotBlank
        String contentType,

        @Positive
        long sizeBytes,

        @NotBlank
        String originalName,

        boolean thumbnail

) {}
