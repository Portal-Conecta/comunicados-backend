package com.portal.conecta.comunicados.module.comunicado.infrastructure.messaging.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.port.AnnouncementNotificationPublisher;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.messaging.config.NotificationPublisherProperties;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.messaging.dto.AnnouncementNotificationPayload;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.messaging.dto.AnnouncementNotificationPayload.NotificationScopePayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnnouncementNotificationPublisherAdapter implements AnnouncementNotificationPublisher {

    private static final String SOURCE = "comunicados-api";
    private static final String EVENT_TYPE = "announcement.published";

    private final RabbitTemplate rabbitTemplate;
    private final NotificationPublisherProperties properties;

    @Override
    public void publish(Announcement announcement, List<AnnouncementDestination> destinations) {
        AnnouncementNotificationPayload payload = buildPayload(announcement, destinations);

        rabbitTemplate.convertAndSend(properties.exchange(), properties.routingKey(), payload);

        log.info(
                "Notificação de comunicado publicada. messageId={}, announcementId={}, scopes={}",
                payload.messageId(),
                announcement.getId(),
                payload.scope().size()
        );
    }

    private AnnouncementNotificationPayload buildPayload(
            Announcement announcement,
            List<AnnouncementDestination> destinations
    ) {
        return new AnnouncementNotificationPayload(
                UUID.randomUUID().toString(),
                announcement.getId().toString(),
                SOURCE,
                EVENT_TYPE,
                Instant.now(),
                announcement.getTitle(),
                announcement.getDescription(),
                toScopes(destinations),
                List.of(),
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
}
