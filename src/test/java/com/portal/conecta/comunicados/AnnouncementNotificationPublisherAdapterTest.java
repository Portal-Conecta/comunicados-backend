package com.portal.conecta.comunicados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.messaging.adapter.AnnouncementNotificationPublisherAdapter;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.messaging.config.NotificationPublisherProperties;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.messaging.dto.AnnouncementNotificationPayload;
import com.portal.conecta.comunicados.shared.context.UserType;


@ExtendWith(MockitoExtension.class)
class AnnouncementNotificationPublisherAdapterTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private AnnouncementNotificationPublisherAdapter adapter;

    @BeforeEach
    void setUp() {
        NotificationPublisherProperties properties =
                new NotificationPublisherProperties("notifications.exchange", "notification.requested");
        adapter = new AnnouncementNotificationPublisherAdapter(rabbitTemplate, properties);
    }

    @Test
    void publish_withClassDestination_shouldMapToClassScope() {
        UUID classId = UUID.randomUUID();
        Announcement announcement = announcement();
        AnnouncementDestination destination = destination(AnnouncementDestinationType.CLASS, classId);

        adapter.publish(announcement, List.of(destination), List.of());

        AnnouncementNotificationPayload payload = capturePayload();
        assertThat(payload.scope()).hasSize(1);
        assertThat(payload.scope().get(0).type()).isEqualTo("CLASS");
        assertThat(payload.scope().get(0).correlationId()).isEqualTo(classId.toString());
    }

    @Test
    void publish_withCourseDestination_shouldMapToCourseScope() {
        UUID courseId = UUID.randomUUID();
        Announcement announcement = announcement();
        AnnouncementDestination destination = destination(AnnouncementDestinationType.COURSE, courseId);

        adapter.publish(announcement, List.of(destination), List.of());

        AnnouncementNotificationPayload payload = capturePayload();
        assertThat(payload.scope()).hasSize(1);
        assertThat(payload.scope().get(0).type()).isEqualTo("COURSE");
        assertThat(payload.scope().get(0).correlationId()).isEqualTo(courseId.toString());
    }

    @Test
    void publish_withUserDestination_shouldMapToUserScope() {
        UUID userId = UUID.randomUUID();
        Announcement announcement = announcement();
        AnnouncementDestination destination = destination(AnnouncementDestinationType.USER, userId);

        adapter.publish(announcement, List.of(destination), List.of());

        AnnouncementNotificationPayload payload = capturePayload();
        assertThat(payload.scope()).hasSize(1);
        assertThat(payload.scope().get(0).type()).isEqualTo("USER");
        assertThat(payload.scope().get(0).correlationId()).isEqualTo(userId.toString());
    }

    @Test
    void publish_withGeneralDestination_shouldMapToGlobalScopeWithNullCorrelationId() {
        Announcement announcement = announcement();
        AnnouncementDestination destination = destination(AnnouncementDestinationType.GENERAL, null);

        adapter.publish(announcement, List.of(destination), List.of());

        AnnouncementNotificationPayload payload = capturePayload();
        assertThat(payload.scope()).hasSize(1);
        assertThat(payload.scope().get(0).type()).isEqualTo("GLOBAL");
        assertThat(payload.scope().get(0).correlationId()).isNull();
    }

    @Test
    void publish_shouldSendToCorrectExchangeAndRoutingKey() {
        adapter.publish(announcement(), List.of(destination(AnnouncementDestinationType.GENERAL, null)), List.of());

        verify(rabbitTemplate).convertAndSend(
                eq("notifications.exchange"),
                eq("notification.requested"),
                any(AnnouncementNotificationPayload.class)
        );
    }

    @Test
    void publish_shouldPopulateMetadataWithAnnouncementId() {
        Announcement announcement = announcement();

        adapter.publish(announcement, List.of(destination(AnnouncementDestinationType.GENERAL, null)), List.of());

        AnnouncementNotificationPayload payload = capturePayload();
        assertThat(payload.metadata()).containsEntry("announcementId", announcement.getId().toString());
    }

    @Test
    void publish_shouldSetCorrectSourceAndEventType() {
        adapter.publish(announcement(), List.of(destination(AnnouncementDestinationType.GENERAL, null)), List.of());

        AnnouncementNotificationPayload payload = capturePayload();
        assertThat(payload.source()).isEqualTo("comunicados-service");
        assertThat(payload.eventType()).isEqualTo("announcement.published");
        assertThat(payload.body()).isEqualTo("Descrição do comunicado");
    }

    @Test
    void publish_withMultipleDestinations_shouldMapAllToScopes() {
        UUID classId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        Announcement announcement = announcement();

        adapter.publish(announcement, List.of(
                destination(AnnouncementDestinationType.CLASS, classId),
                destination(AnnouncementDestinationType.COURSE, courseId),
                destination(AnnouncementDestinationType.GENERAL, null)
        ), List.of());

        AnnouncementNotificationPayload payload = capturePayload();
        assertThat(payload.scope()).hasSize(3);
        assertThat(payload.scope()).extracting("type")
                .containsExactly("CLASS", "COURSE", "GLOBAL");
    }

    @Test
    void publish_withNoRoles_shouldSendEmptyFilters() {
        adapter.publish(announcement(), List.of(destination(AnnouncementDestinationType.GENERAL, null)), List.of());

        AnnouncementNotificationPayload payload = capturePayload();
        assertThat(payload.filters()).isEmpty();
    }

    @Test
    void publish_withRoles_shouldMapToRoleFilters() {
        adapter.publish(
                announcement(),
                List.of(destination(AnnouncementDestinationType.GENERAL, null)),
                List.of(UserType.TEACHER)
        );

        AnnouncementNotificationPayload payload = capturePayload();
        assertThat(payload.filters()).hasSize(1);
        assertThat(payload.filters().get(0).type()).isEqualTo("ROLE");
        assertThat(payload.filters().get(0).value()).isEqualTo("TEACHER");
    }

    @Test
    void publish_withStudentRole_shouldAlsoNotifyRepresentative() {
        adapter.publish(
                announcement(),
                List.of(destination(AnnouncementDestinationType.GENERAL, null)),
                List.of(UserType.STUDENT)
        );

        AnnouncementNotificationPayload payload = capturePayload();
        assertThat(payload.filters())
                .extracting(AnnouncementNotificationPayload.NotificationFilterPayload::value)
                .containsExactlyInAnyOrder("STUDENT", "REPRESENTATIVE");
    }

    private AnnouncementNotificationPayload capturePayload() {
        ArgumentCaptor<AnnouncementNotificationPayload> captor =
                ArgumentCaptor.forClass(AnnouncementNotificationPayload.class);
        verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), captor.capture());
        return captor.getValue();
    }

    private Announcement announcement() {
        Instant now = Instant.now();
        return Announcement.builder()
                .id(UUID.randomUUID())
                .title("Comunicado Teste")
                .description("<p>Descrição do comunicado</p>")
                .descriptionPlain("Descrição do comunicado")
                .origin(AnnouncementOrigin.SENAI)
                .status(AnnouncementStatus.PUBLISHED)
                .pinned(false)
                .createdByUserId(UUID.randomUUID())
                .createdByUserType(UserType.SENAI)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private AnnouncementDestination destination(AnnouncementDestinationType type, UUID referenceId) {
        return AnnouncementDestination.builder()
                .id(UUID.randomUUID())
                .type(type)
                .referenceId(referenceId)
                .build();
    }
}
