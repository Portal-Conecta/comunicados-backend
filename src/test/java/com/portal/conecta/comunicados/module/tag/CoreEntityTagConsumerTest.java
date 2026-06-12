package com.portal.conecta.comunicados.module.tag;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.portal.conecta.comunicados.module.tag.application.usecase.DeactivateTagUseCase;
import com.portal.conecta.comunicados.module.tag.application.usecase.UpsertTagFromCoreUseCase;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.model.ProcessedEvent;
import com.portal.conecta.comunicados.module.tag.domain.port.ProcessedEventRepository;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.consumer.CoreEntityTagConsumer;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.dto.CoreEntityEventEnvelope;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.dto.CoreEntityEventPayload;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.exception.InvalidCoreEntityEventException;

@ExtendWith(MockitoExtension.class)
class CoreEntityTagConsumerTest {

    @Mock
    private UpsertTagFromCoreUseCase upsertTagFromCoreUseCase;

    @Mock
    private DeactivateTagUseCase deactivateTagUseCase;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private CoreEntityTagConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new CoreEntityTagConsumer(
                upsertTagFromCoreUseCase,
                deactivateTagUseCase,
                processedEventRepository
        );
    }

    @Test
    void shouldUpsertTagOnClassCreatedEvent() {
        UUID eventId = UUID.randomUUID();
        UUID hubEntityId = UUID.randomUUID();
        CoreEntityEventEnvelope envelope = envelope(
                eventId,
                "core.class.created",
                hubEntityId,
                "MI78 - Manhã",
                true
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        consumer.handle(envelope);

        verify(upsertTagFromCoreUseCase).execute(any());
        verify(deactivateTagUseCase, never()).execute(any());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void shouldDeactivateTagOnClassDeactivatedEvent() {
        UUID eventId = UUID.randomUUID();
        UUID hubEntityId = UUID.randomUUID();
        CoreEntityEventEnvelope envelope = envelope(
                eventId,
                "core.class.deactivated",
                hubEntityId,
                "MI78 - Manhã",
                false
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        consumer.handle(envelope);

        verify(deactivateTagUseCase).execute(any());
        verify(upsertTagFromCoreUseCase, never()).execute(any());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void shouldIgnoreDuplicateEventId() {
        UUID eventId = UUID.randomUUID();
        CoreEntityEventEnvelope envelope = envelope(
                eventId,
                "core.class.created",
                UUID.randomUUID(),
                "MI78 - Manhã",
                true
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        consumer.handle(envelope);

        verify(upsertTagFromCoreUseCase, never()).execute(any());
        verify(deactivateTagUseCase, never()).execute(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldRejectUnknownEventType() {
        UUID eventId = UUID.randomUUID();
        CoreEntityEventEnvelope envelope = envelope(
                eventId,
                "core.class.unknown",
                UUID.randomUUID(),
                "MI78 - Manhã",
                true
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        assertThatThrownBy(() -> consumer.handle(envelope))
                .isInstanceOf(InvalidCoreEntityEventException.class)
                .hasMessageContaining("eventType não suportado");

        verify(processedEventRepository, never()).save(any());
    }

    private CoreEntityEventEnvelope envelope(
            UUID eventId,
            String eventType,
            UUID hubEntityId,
            String name,
            boolean active
    ) {
        return new CoreEntityEventEnvelope(
                eventId,
                eventType,
                Instant.parse("2026-06-12T10:00:00Z"),
                "portal-core",
                new CoreEntityEventPayload(
                        hubEntityId,
                        TagEntityType.CLASS,
                        name,
                        active,
                        Map.of()
                )
        );
    }
}
