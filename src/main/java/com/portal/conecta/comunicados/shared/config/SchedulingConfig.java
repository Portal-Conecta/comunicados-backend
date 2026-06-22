package com.portal.conecta.comunicados.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita o agendamento de tarefas (@Scheduled) da aplicação — ver
 * {@code AutoPublishScheduledAnnouncementsJob} (#135).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
