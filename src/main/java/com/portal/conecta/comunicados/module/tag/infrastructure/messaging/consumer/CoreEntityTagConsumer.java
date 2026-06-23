package com.portal.conecta.comunicados.module.tag.infrastructure.messaging.consumer;

import java.time.Instant;
import java.util.Set;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.portal.conecta.comunicados.module.tag.application.command.DeactivateTagCommand;
import com.portal.conecta.comunicados.module.tag.application.command.UpsertTagFromCoreCommand;
import com.portal.conecta.comunicados.module.tag.application.usecase.DeactivateTagUseCase;
import com.portal.conecta.comunicados.module.tag.application.usecase.UpsertTagFromCoreUseCase;
import com.portal.conecta.comunicados.module.tag.domain.model.ProcessedEvent;
import com.portal.conecta.comunicados.module.tag.domain.port.ProcessedEventRepository;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.dto.CoreEntityEventEnvelope;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.exception.InvalidCoreEntityEventException;

import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class CoreEntityTagConsumer {

    private static final Set<String> UPSERT_EVENTS = Set.of(
            "course.created",
            "course.updated",
            "turma.created"
    );

    private static final Set<String> DEACTIVATION_EVENTS = Set.of(
            "course.deleted",
            "turma.deleted",
            "course.deactivated",
            "turma.deactivated"
    );

    private final UpsertTagFromCoreUseCase upsertTagFromCoreUseCase;
    private final DeactivateTagUseCase deactivateTagUseCase;
    private final ProcessedEventRepository processedEventRepository;

    @RabbitListener(queues = "${app.messaging.core.queue}")
    @Transactional
    public void handle(CoreEntityEventEnvelope envelope) {
        validateEnvelope(envelope);

        if (processedEventRepository.existsById(envelope.eventId())) {
            return;
        }

        String eventType = envelope.eventType().trim();

        if (isDeactivation(eventType)) {
            deactivateTagUseCase.execute(DeactivateTagCommand.from(envelope));
        } else if (isUpsert(eventType)) {
            upsertTagFromCoreUseCase.execute(UpsertTagFromCoreCommand.from(envelope));
        } else {
            throw new InvalidCoreEntityEventException("eventType não suportado: " + eventType);
        }

        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(envelope.eventId())
                .processedAt(Instant.now())
                .build());
    }

    private void validateEnvelope(CoreEntityEventEnvelope envelope) {
        if (envelope == null) {
            throw new InvalidCoreEntityEventException("envelope do evento é obrigatório.");
        }
        if (isBlank(envelope.eventId())) {
            throw new InvalidCoreEntityEventException("eventId é obrigatório.");
        }
        if (isBlank(envelope.correlationId())) {
            throw new InvalidCoreEntityEventException("correlationId é obrigatório.");
        }
        if (isBlank(envelope.source())) {
            throw new InvalidCoreEntityEventException("source é obrigatório.");
        }
        if (isBlank(envelope.eventType())) {
            throw new InvalidCoreEntityEventException("eventType é obrigatório.");
        }
        if (envelope.occurredAt() == null) {
            throw new InvalidCoreEntityEventException("occurredAt é obrigatório.");
        }
        if (isBlank(envelope.entityType())) {
            throw new InvalidCoreEntityEventException("entityType é obrigatório.");
        }
        if (isBlank(envelope.entityId())) {
            throw new InvalidCoreEntityEventException("entityId é obrigatório.");
        }
    }

    private boolean isUpsert(String eventType) {
        return UPSERT_EVENTS.contains(eventType);
    }

    private boolean isDeactivation(String eventType) {
        return DEACTIVATION_EVENTS.contains(eventType) || eventType.endsWith(".deactivated");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}