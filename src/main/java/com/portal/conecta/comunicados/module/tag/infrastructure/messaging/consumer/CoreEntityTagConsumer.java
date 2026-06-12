package com.portal.conecta.comunicados.module.tag.infrastructure.messaging.consumer;

import java.time.Instant;

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

        String eventType = envelope.eventType();

        if (isDeactivated(eventType)) {
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
        if (envelope.eventId() == null) {
            throw new InvalidCoreEntityEventException("eventId é obrigatório.");
        }
        if (envelope.eventType() == null || envelope.eventType().isBlank()) {
            throw new InvalidCoreEntityEventException("eventType é obrigatório.");
        }
    }

    private boolean isUpsert(String eventType) {
        return eventType.endsWith(".created") || eventType.endsWith(".updated");
    }

    private boolean isDeactivated(String eventType) {
        return eventType.endsWith(".deactivated");
    }
}
