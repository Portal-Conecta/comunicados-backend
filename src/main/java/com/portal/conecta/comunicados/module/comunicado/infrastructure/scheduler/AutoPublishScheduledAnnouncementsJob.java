package com.portal.conecta.comunicados.module.comunicado.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.application.usecase.PublishDueAnnouncementsUseCase;

import lombok.RequiredArgsConstructor;

/**
 * Dispara periodicamente a publicação automática de comunicados agendados (#135).
 *
 * <p>Frequência configurável via {@code app.scheduling.auto-publish.cron} (default: a cada minuto).
 * Desligável com {@code app.scheduling.auto-publish.enabled=false} (ex.: profile de teste).</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.scheduling.auto-publish",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class AutoPublishScheduledAnnouncementsJob {

    private static final Logger log = LoggerFactory.getLogger(AutoPublishScheduledAnnouncementsJob.class);

    private final PublishDueAnnouncementsUseCase publishDueAnnouncementsUseCase;

    @Scheduled(cron = "${app.scheduling.auto-publish.cron:0 * * * * *}")
    public void run() {
        int published = publishDueAnnouncementsUseCase.execute();
        if (published > 0) {
            log.info("Publicação automática: {} comunicado(s) agendado(s) publicado(s).", published);
        }
    }
}
