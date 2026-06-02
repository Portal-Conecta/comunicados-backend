package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAnnouncementFileRequest(

    @NotNull
    UUID announcementId,

    @NotBlank
    String originalName,

    @NotBlank
    String s3Key,

    @NotBlank
    String s3Bucket,

    @NotBlank
    String contentType,

    @NotNull
    AnnouncementFileType type,

    @NotNull
    @Min(1)
    Long sizeBytes,

    Boolean isThumbnail

) {}