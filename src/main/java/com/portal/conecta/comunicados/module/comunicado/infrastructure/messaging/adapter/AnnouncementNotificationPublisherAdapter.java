package com.portal.conecta.comunicados.module.comunicado.infrastructure.messaging.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.port.AnnouncementNotificationPublisher;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.messaging.config.NotificationPublisherProperties;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.messaging.dto.AnnouncementNotificationPayload;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.messaging.dto.AnnouncementNotificationPayload.NotificationFilterPayload;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.messaging.dto.AnnouncementNotificationPayload.NotificationScopePayload;
import com.portal.conecta.comunicados.shared.context.UserType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class AnnouncementNotificationPublisherAdapter implements AnnouncementNotificationPublisher {

    private static final String SOURCE = "comunicados-service";
    private static final String EVENT_TYPE = "announcement.published";
    private static final String ROLE_FILTER_TYPE = "ROLE";
    private static final int MAX_ATTEMPTS = 3;
    private static final long BACKOFF_MS = 200L;

    private final RabbitTemplate rabbitTemplate;
    private final NotificationPublisherProperties properties;

    @Override
    public void publish(Announcement announcement, List<AnnouncementDestination> destinations, List<UserType> roles) {
        AnnouncementNotificationPayload payload = buildPayload(announcement, destinations, roles);

        Exception last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                rabbitTemplate.convertAndSend(properties.exchange(), properties.routingKey(), payload);
                log.info(
                        "Notificação de comunicado publicada. messageId={}, announcementId={}, scopes={}, attempt={}",
                        payload.messageId(),
                        announcement.getId(),
                        payload.scope().size(),
                        attempt
                );
                return;
            } catch (Exception e) {
                last = e;
                log.warn(
                        "Tentativa {}/{} de notificação falhou. announcementId={}",
                        attempt,
                        MAX_ATTEMPTS,
                        announcement.getId(),
                        e
                );
                if (attempt < MAX_ATTEMPTS) {
                    try {
                        Thread.sleep(BACKOFF_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Interrompido ao republicar notificação.", ie);
                    }
                }
            }
        }
        throw new IllegalStateException(
                "Falha ao publicar notificação após " + MAX_ATTEMPTS + " tentativas.", last);
    }

    private AnnouncementNotificationPayload buildPayload(
            Announcement announcement,
            List<AnnouncementDestination> destinations,
            List<UserType> roles
    ) {
        return new AnnouncementNotificationPayload(
                UUID.randomUUID().toString(),
                announcement.getId().toString(),
                SOURCE,
                EVENT_TYPE,
                Instant.now(),
                announcement.getTitle(),
                announcement.getDescriptionPlain() != null ? announcement.getDescriptionPlain() : "",
                toScopes(destinations),
                toFilters(roles),
                Map.of("announcementId", announcement.getId().toString())
        );
    }

    private List<NotificationScopePayload> toScopes(List<AnnouncementDestination> destinations) {
        return destinations.stream()
                .map(this::toScope)
                .toList();
    }

    private NotificationScopePayload toScope(AnnouncementDestination destination) {
        if (destination.getType() == AnnouncementDestinationType.GENERAL) {
            return new NotificationScopePayload("GLOBAL", null);
        }
        return new NotificationScopePayload(
                destination.getType().name(),
                destination.getReferenceId().toString()
        );
    }

    private List<NotificationFilterPayload> toFilters(List<UserType> roles) {
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .map(role -> new NotificationFilterPayload(ROLE_FILTER_TYPE, role.name()))
                .toList();
    }
}
