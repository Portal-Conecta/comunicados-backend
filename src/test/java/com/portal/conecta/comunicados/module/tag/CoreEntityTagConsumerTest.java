package com.portal.conecta.comunicados.module.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.portal.conecta.comunicados.module.tag.application.command.DeactivateTagCommand;
import com.portal.conecta.comunicados.module.tag.application.command.UpsertTagFromCoreCommand;
import com.portal.conecta.comunicados.module.tag.application.usecase.DeactivateTagUseCase;
import com.portal.conecta.comunicados.module.tag.application.usecase.UpsertTagFromCoreUseCase;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.model.ProcessedEvent;
import com.portal.conecta.comunicados.module.tag.domain.port.ProcessedEventRepository;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.consumer.CoreEntityTagConsumer;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.dto.CoreEntityEventEnvelope;
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
    void shouldUpsertCourseCreatedEvent() {
        CoreEntityEventEnvelope envelope = envelope(
                "evt-course-created",
                "course.created",
                "course",
                "course-123",
                "MIDS",
                "MIDS"
        );

        when(processedEventRepository.existsById("evt-course-created")).thenReturn(false);

        consumer.handle(envelope);

        ArgumentCaptor<UpsertTagFromCoreCommand> commandCaptor =
                ArgumentCaptor.forClass(UpsertTagFromCoreCommand.class);
        ArgumentCaptor<ProcessedEvent> eventCaptor =
                ArgumentCaptor.forClass(ProcessedEvent.class);

        verify(upsertTagFromCoreUseCase).execute(commandCaptor.capture());
        verify(deactivateTagUseCase, never()).execute(any());
        verify(processedEventRepository).save(eventCaptor.capture());

        UpsertTagFromCoreCommand command = commandCaptor.getValue();
        assertThat(command.hubEntityId()).isEqualTo("course-123");
        assertThat(command.entityType()).isEqualTo(TagEntityType.COURSE);
        assertThat(command.name()).isEqualTo("MIDS");
        assertThat(command.active()).isTrue();

        assertThat(eventCaptor.getValue().getEventId()).isEqualTo("evt-course-created");
    }

    @Test
    void shouldUpsertCourseUpdatedEvent() {
        CoreEntityEventEnvelope envelope = envelope(
                "evt-course-updated",
                "course.updated",
                "course",
                "course-123",
                "MIDS",
                "MIDS atualizado"
        );

        when(processedEventRepository.existsById("evt-course-updated")).thenReturn(false);

        consumer.handle(envelope);

        ArgumentCaptor<UpsertTagFromCoreCommand> commandCaptor =
                ArgumentCaptor.forClass(UpsertTagFromCoreCommand.class);

        verify(upsertTagFromCoreUseCase).execute(commandCaptor.capture());
        verify(deactivateTagUseCase, never()).execute(any());
        verify(processedEventRepository).save(any(ProcessedEvent.class));

        UpsertTagFromCoreCommand command = commandCaptor.getValue();
        assertThat(command.hubEntityId()).isEqualTo("course-123");
        assertThat(command.entityType()).isEqualTo(TagEntityType.COURSE);
        assertThat(command.name()).isEqualTo("MIDS atualizado");
    }

    @Test
    void shouldUpsertTurmaCreatedEventAsClassTag() {
        CoreEntityEventEnvelope envelope = envelope(
                "evt-turma-created",
                "turma.created",
                "turma",
                "turma-78",
                null,
                "MIDS-78"
        );

        when(processedEventRepository.existsById("evt-turma-created")).thenReturn(false);

        consumer.handle(envelope);

        ArgumentCaptor<UpsertTagFromCoreCommand> commandCaptor =
                ArgumentCaptor.forClass(UpsertTagFromCoreCommand.class);

        verify(upsertTagFromCoreUseCase).execute(commandCaptor.capture());
        verify(deactivateTagUseCase, never()).execute(any());
        verify(processedEventRepository).save(any(ProcessedEvent.class));

        UpsertTagFromCoreCommand command = commandCaptor.getValue();
        assertThat(command.hubEntityId()).isEqualTo("turma-78");
        assertThat(command.entityType()).isEqualTo(TagEntityType.CLASS);
        assertThat(command.name()).isEqualTo("MIDS-78");
        assertThat(command.active()).isTrue();
    }

    @Test
    void shouldDeactivateCourseDeletedEvent() {
        CoreEntityEventEnvelope envelope = envelope(
                "evt-course-deleted",
                "course.deleted",
                "course",
                "course-123",
                "MIDS",
                null
        );

        when(processedEventRepository.existsById("evt-course-deleted")).thenReturn(false);

        consumer.handle(envelope);

        ArgumentCaptor<DeactivateTagCommand> commandCaptor =
                ArgumentCaptor.forClass(DeactivateTagCommand.class);

        verify(deactivateTagUseCase).execute(commandCaptor.capture());
        verify(upsertTagFromCoreUseCase, never()).execute(any());
        verify(processedEventRepository).save(any(ProcessedEvent.class));

        DeactivateTagCommand command = commandCaptor.getValue();
        assertThat(command.hubEntityId()).isEqualTo("course-123");
        assertThat(command.entityType()).isEqualTo(TagEntityType.COURSE);
    }

    @Test
    void shouldDeactivateTurmaDeletedEventAsClassTag() {
        CoreEntityEventEnvelope envelope = envelope(
                "evt-turma-deleted",
                "turma.deleted",
                "turma",
                "turma-78",
                null,
                "MIDS-78"
        );

        when(processedEventRepository.existsById("evt-turma-deleted")).thenReturn(false);

        consumer.handle(envelope);

        ArgumentCaptor<DeactivateTagCommand> commandCaptor =
                ArgumentCaptor.forClass(DeactivateTagCommand.class);

        verify(deactivateTagUseCase).execute(commandCaptor.capture());
        verify(upsertTagFromCoreUseCase, never()).execute(any());
        verify(processedEventRepository).save(any(ProcessedEvent.class));

        DeactivateTagCommand command = commandCaptor.getValue();
        assertThat(command.hubEntityId()).isEqualTo("turma-78");
        assertThat(command.entityType()).isEqualTo(TagEntityType.CLASS);
    }

    @Test
    void shouldKeepTemporaryCompatibilityWithDeactivatedSuffix() {
        CoreEntityEventEnvelope envelope = envelope(
                "evt-course-deactivated",
                "course.deactivated",
                "course",
                "course-123",
                "MIDS",
                "MIDS"
        );

        when(processedEventRepository.existsById("evt-course-deactivated")).thenReturn(false);

        consumer.handle(envelope);

        verify(deactivateTagUseCase).execute(any(DeactivateTagCommand.class));
        verify(upsertTagFromCoreUseCase, never()).execute(any());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void shouldIgnoreDuplicateEventId() {
        CoreEntityEventEnvelope envelope = envelope(
                "evt-duplicated",
                "course.created",
                "course",
                "course-123",
                "MIDS",
                "MIDS"
        );

        when(processedEventRepository.existsById("evt-duplicated")).thenReturn(true);

        consumer.handle(envelope);

        verify(upsertTagFromCoreUseCase, never()).execute(any());
        verify(deactivateTagUseCase, never()).execute(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldRejectUnknownEventType() {
        CoreEntityEventEnvelope envelope = envelope(
                "evt-unknown",
                "turma.updated",
                "turma",
                "turma-78",
                null,
                "MIDS-78"
        );

        when(processedEventRepository.existsById("evt-unknown")).thenReturn(false);

        assertThatThrownBy(() -> consumer.handle(envelope))
                .isInstanceOf(InvalidCoreEntityEventException.class)
                .hasMessageContaining("eventType");

        verify(upsertTagFromCoreUseCase, never()).execute(any());
        verify(deactivateTagUseCase, never()).execute(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldRejectInvalidEnvelopeBeforePersistingProcessedEvent() {
        CoreEntityEventEnvelope envelope = new CoreEntityEventEnvelope(
                "evt-invalid",
                "corr-invalid",
                "core-api",
                "course.created",
                Instant.parse("2026-06-19T18:00:00Z"),
                "course",
                "",
                "MIDS",
                "MIDS"
        );

        assertThatThrownBy(() -> consumer.handle(envelope))
                .isInstanceOf(InvalidCoreEntityEventException.class)
                .hasMessageContaining("entityId");

        verify(upsertTagFromCoreUseCase, never()).execute(any());
        verify(deactivateTagUseCase, never()).execute(any());
        verify(processedEventRepository, never()).existsById(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldRejectUpsertWithoutNameOrCodeAndNotCreatePartialTag() {
        CoreEntityEventEnvelope envelope = envelope(
                "evt-without-name",
                "course.created",
                "course",
                "course-123",
                null,
                null
        );

        when(processedEventRepository.existsById("evt-without-name")).thenReturn(false);

        assertThatThrownBy(() -> consumer.handle(envelope))
                .isInstanceOf(InvalidCoreEntityEventException.class)
                .hasMessageContaining("name ou code");

        verify(upsertTagFromCoreUseCase, never()).execute(any());
        verify(deactivateTagUseCase, never()).execute(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldConsumeOfficialCourseCreatedJsonExample() throws Exception {
        CoreEntityEventEnvelope envelope = objectMapper().readValue("""
                {
                  "eventId": "evt-01JYTAGCOURSECREATED",
                  "correlationId": "corr-01JYTAGCOURSECREATED",
                  "source": "core-api",
                  "eventType": "course.created",
                  "occurredAt": "2026-06-19T18:00:00Z",
                  "entityType": "course",
                  "entityId": "course-123",
                  "code": "MIDS",
                  "name": "MIDS"
                }
                """, CoreEntityEventEnvelope.class);

        when(processedEventRepository.existsById("evt-01JYTAGCOURSECREATED")).thenReturn(false);

        consumer.handle(envelope);

        ArgumentCaptor<UpsertTagFromCoreCommand> commandCaptor =
                ArgumentCaptor.forClass(UpsertTagFromCoreCommand.class);

        verify(upsertTagFromCoreUseCase).execute(commandCaptor.capture());
        verify(processedEventRepository).save(any(ProcessedEvent.class));

        assertThat(commandCaptor.getValue().entityType()).isEqualTo(TagEntityType.COURSE);
        assertThat(commandCaptor.getValue().hubEntityId()).isEqualTo("course-123");
    }

    @Test
    void shouldConsumeOfficialTurmaCreatedJsonExample() throws Exception {
        CoreEntityEventEnvelope envelope = objectMapper().readValue("""
                {
                  "eventId": "evt-01JYTAGTURMACREATED",
                  "correlationId": "corr-01JYTAGTURMACREATED",
                  "source": "core-api",
                  "eventType": "turma.created",
                  "occurredAt": "2026-06-19T18:30:00Z",
                  "entityType": "turma",
                  "entityId": "turma-78",
                  "name": "MIDS-78"
                }
                """, CoreEntityEventEnvelope.class);

        when(processedEventRepository.existsById("evt-01JYTAGTURMACREATED")).thenReturn(false);

        consumer.handle(envelope);

        ArgumentCaptor<UpsertTagFromCoreCommand> commandCaptor =
                ArgumentCaptor.forClass(UpsertTagFromCoreCommand.class);

        verify(upsertTagFromCoreUseCase).execute(commandCaptor.capture());
        verify(processedEventRepository).save(any(ProcessedEvent.class));

        assertThat(commandCaptor.getValue().entityType()).isEqualTo(TagEntityType.CLASS);
        assertThat(commandCaptor.getValue().hubEntityId()).isEqualTo("turma-78");
    }

    @Test
    void shouldConsumeOfficialTurmaDeletedJsonExample() throws Exception {
        CoreEntityEventEnvelope envelope = objectMapper().readValue("""
                {
                  "eventId": "evt-01JYTAGTURMADELETED",
                  "correlationId": "corr-01JYTAGTURMADELETED",
                  "source": "core-api",
                  "eventType": "turma.deleted",
                  "occurredAt": "2026-06-19T18:40:00Z",
                  "entityType": "turma",
                  "entityId": "turma-78",
                  "name": "MIDS-78"
                }
                """, CoreEntityEventEnvelope.class);

        when(processedEventRepository.existsById("evt-01JYTAGTURMADELETED")).thenReturn(false);

        consumer.handle(envelope);

        ArgumentCaptor<DeactivateTagCommand> commandCaptor =
                ArgumentCaptor.forClass(DeactivateTagCommand.class);

        verify(deactivateTagUseCase).execute(commandCaptor.capture());
        verify(processedEventRepository).save(any(ProcessedEvent.class));

        assertThat(commandCaptor.getValue().entityType()).isEqualTo(TagEntityType.CLASS);
        assertThat(commandCaptor.getValue().hubEntityId()).isEqualTo("turma-78");
    }

    private CoreEntityEventEnvelope envelope(
            String eventId,
            String eventType,
            String entityType,
            String entityId,
            String code,
            String name
    ) {
        return new CoreEntityEventEnvelope(
                eventId,
                "corr-" + eventId,
                "core-api",
                eventType,
                Instant.parse("2026-06-19T18:00:00Z"),
                entityType,
                entityId,
                code,
                name
        );
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}