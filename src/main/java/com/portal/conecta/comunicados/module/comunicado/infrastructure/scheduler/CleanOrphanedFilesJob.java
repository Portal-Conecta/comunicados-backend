package com.portal.conecta.comunicados.module.comunicado.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.application.usecase.CleanOrphanedFilesUseCase;

import lombok.RequiredArgsConstructor;

/**
 * Remove registros AnnouncementFile PENDING abandonados (upload nunca concluído).
 * Confirma ausência no bucket B via HeadObject antes de marcar FAILED.
 * Intervalo e limite de idade configuráveis via app.scheduling.cleanup-orphans.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.scheduling.cleanup-orphans",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class CleanOrphanedFilesJob {

    private static final Logger log = LoggerFactory.getLogger(CleanOrphanedFilesJob.class);

    private final CleanOrphanedFilesUseCase cleanOrphanedFilesUseCase;

    @Value("${app.scheduling.cleanup-orphans.max-age-hours:24}")
    private int maxAgeHours;

    @Scheduled(cron = "${app.scheduling.cleanup-orphans.cron:0 0 * * * *}")
    public void run() {
        int cleaned = cleanOrphanedFilesUseCase.execute(maxAgeHours);
        if (cleaned > 0) {
            log.info("Limpeza de órfãos: {} AnnouncementFile(s) PENDING expirado(s) marcado(s) como FAILED.", cleaned);
        }
    }
}
