package com.portal.conecta.comunicados.module.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.portal.conecta.comunicados.module.tag.application.usecase.DeactivateTagUseCase;
import com.portal.conecta.comunicados.module.tag.application.usecase.UpsertTagFromCoreUseCase;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.domain.port.ProcessedEventRepository;
import com.portal.conecta.comunicados.module.tag.domain.port.TagRepository;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.consumer.CoreEntityTagConsumer;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.dto.CoreEntityEventEnvelope;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.exception.InvalidCoreEntityEventException;

/**
 * Testes de integração do fluxo RabbitMQ → persistência de tag.
 *
 * O consumer é instanciado diretamente (sem broker real), usando use-cases e repositórios reais
 * com banco H2 em memória. Isso cobre os cenários da issue #152 sem dependência de infraestrutura
 * externa durante a execução em CI.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TagRabbitMqIntegrationTest {

    @Autowired
    private UpsertTagFromCoreUseCase upsertTagFromCoreUseCase;

    @Autowired
    private DeactivateTagUseCase deactivateTagUseCase;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private TagRepository tagRepository;

    private CoreEntityTagConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new CoreEntityTagConsumer(upsertTagFromCoreUseCase, deactivateTagUseCase, processedEventRepository);
    }

    @Test
    void courseCreatedEvent_shouldPersistCourseTag() {
        consumer.handle(envelope("evt-course-int-01", "course.created", "course", "course-abc", "MIDS", "MIDS"));

        Optional<Tag> tag = tagRepository.findByEntityTypeAndHubEntityId(TagEntityType.COURSE, "course-abc");
        assertThat(tag).isPresent();
        assertThat(tag.get().getName()).isEqualTo("MIDS");
        assertThat(tag.get().isActive()).isTrue();
        assertThat(processedEventRepository.existsById("evt-course-int-01")).isTrue();
    }

    @Test
    void classCreatedEvent_shouldPersistClassTag() {
        consumer.handle(envelope("evt-class-int-01", "class.created", "class", "class-78", null, "MIDS-78"));

        Optional<Tag> tag = tagRepository.findByEntityTypeAndHubEntityId(TagEntityType.CLASS, "class-78");
        assertThat(tag).isPresent();
        assertThat(tag.get().getName()).isEqualTo("MIDS-78");
        assertThat(tag.get().isActive()).isTrue();
        assertThat(processedEventRepository.existsById("evt-class-int-01")).isTrue();
    }

    @Test
    void duplicateEventId_shouldNotCreateDuplicateTagOrProcessedEvent() {
        consumer.handle(envelope("evt-dup-int-01", "course.created", "course", "course-dup", "MIDS", "MIDS"));

        long tagsBefore = tagRepository.count();
        long processedBefore = processedEventRepository.count();

        consumer.handle(envelope("evt-dup-int-01", "course.created", "course", "course-dup", "MIDS", "MIDS Atualizado"));

        assertThat(tagRepository.count()).isEqualTo(tagsBefore);
        assertThat(processedEventRepository.count()).isEqualTo(processedBefore);
        Optional<Tag> tag = tagRepository.findByEntityTypeAndHubEntityId(TagEntityType.COURSE, "course-dup");
        assertThat(tag).isPresent();
        assertThat(tag.get().getName()).isEqualTo("MIDS");
    }

    @Test
    void invalidEnvelope_emptyEntityId_shouldThrowAndNotPersistAnything() {
        CoreEntityEventEnvelope envelope = new CoreEntityEventEnvelope(
                "evt-invalid-int-01", "corr-01", "core-api", "course.created",
                Instant.parse("2026-06-19T18:00:00Z"), "course", "", "MIDS", "MIDS"
        );
        long tagsBefore = tagRepository.count();

        assertThatThrownBy(() -> consumer.handle(envelope))
                .isInstanceOf(InvalidCoreEntityEventException.class)
                .hasMessageContaining("entityId");

        assertThat(tagRepository.count()).isEqualTo(tagsBefore);
        assertThat(processedEventRepository.existsById("evt-invalid-int-01")).isFalse();
    }

    @Test
    void unsupportedEventType_shouldThrowAndRouteToDeadLetter() {
        long tagsBefore = tagRepository.count();

        assertThatThrownBy(() ->
                consumer.handle(envelope("evt-unknown-int-01", "turma.updated", "turma", "turma-99", null, "Turma"))
        )
                .isInstanceOf(InvalidCoreEntityEventException.class)
                .hasMessageContaining("eventType");

        assertThat(tagRepository.count()).isEqualTo(tagsBefore);
        assertThat(processedEventRepository.existsById("evt-unknown-int-01")).isFalse();
    }

    private CoreEntityEventEnvelope envelope(
            String eventId, String eventType, String entityType, String entityId, String code, String name
    ) {
        return new CoreEntityEventEnvelope(
                eventId, "corr-" + eventId, "core-api", eventType,
                Instant.parse("2026-06-19T18:00:00Z"), entityType, entityId, code, name
        );
    }
}
