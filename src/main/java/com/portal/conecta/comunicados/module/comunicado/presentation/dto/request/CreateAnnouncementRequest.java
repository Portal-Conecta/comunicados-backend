package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateAnnouncementRequest(
        
    @NotBlank
    @Size(max = 255)
    String title,
    
    @NotBlank
    String description,
    
    @NotNull
    AnnouncementOrigin origin,
    
    @NotNull
    AnnouncementStatus status,
    
    Boolean pinned,
    
    @Min(0)
    Short pinnedOrder,
    
    Instant scheduledFor

) {}