package com.portal.conecta.comunicados.module.tag;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.AnnouncementTagRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.specification.AnnouncementSpecifications;
import com.portal.conecta.comunicados.module.tag.application.dto.TagLinkDestinationCommand;
import com.portal.conecta.comunicados.module.tag.application.usecase.AutoLinkTagsByDestinationUseCase;
import com.portal.conecta.comunicados.module.tag.application.usecase.DeactivateTagUseCase;
import com.portal.conecta.comunicados.module.tag.application.usecase.UpsertTagFromCoreUseCase;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.domain.port.ProcessedEventRepository;
import com.portal.conecta.comunicados.module.tag.domain.port.TagRepository;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.consumer.CoreEntityTagConsumer;
import com.portal.conecta.comunicados.module.tag.infrastructure.messaging.dto.CoreEntityEventEnvelope;
import com.portal.conecta.comunicados.shared.context.UserType;

/**
 * Testes de integração end-to-end: evento RabbitMQ → tag → auto-vínculo → filtro GET /api/posts?tagId=.
 * Issue #152, cenários 5 e 6.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TagAutoLinkFlowTest {

    @Autowired
    private UpsertTagFromCoreUseCase upsertTagFromCoreUseCase;

    @Autowired
    private DeactivateTagUseCase deactivateTagUseCase;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private AnnouncementTagRepository announcementTagRepository;

    @Autowired
    private AutoLinkTagsByDestinationUseCase autoLinkTagsByDestinationUseCase;

    private CoreEntityTagConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new CoreEntityTagConsumer(upsertTagFromCoreUseCase, deactivateTagUseCase, processedEventRepository);
    }

    @Test
    void turmaCreatedEvent_thenPublishToClassDestination_shouldAutoLinkAnnouncementTag() {
        UUID turmaId = UUID.randomUUID();

        // Passo 1: consumir evento turma.created → cria tag CLASS com hubEntityId = turmaId
        consumer.handle(turmaEnvelope("evt-flow-int-01", turmaId.toString(), "MIDS-78"));
        Tag classTag = tagRepository.findByEntityTypeAndHubEntityId(TagEntityType.CLASS, turmaId.toString())
                .orElseThrow(() -> new AssertionError("Tag CLASS não foi criada pelo evento"));

        // Passo 2: salvar comunicado publicado
        Announcement announcement = announcementRepository.save(publishedAnnouncement());

        // Passo 3: auto-vincular via destino CLASS apontando para turmaId
        autoLinkTagsByDestinationUseCase.execute(announcement, List.of(
                new TagLinkDestinationCommand(AnnouncementDestinationType.CLASS, turmaId)
        ));

        // Passo 4: verificar que announcement_tag foi criado
        var links = announcementTagRepository.findByAnnouncementId(announcement.getId());
        assertThat(links).hasSize(1);
        assertThat(links.get(0).getTag().getId()).isEqualTo(classTag.getId());
    }

    @Test
    void listAnnouncements_filteredByTagId_afterAutoLink_shouldReturnOnlyLinkedAnnouncement() {
        UUID turmaId = UUID.randomUUID();

        // Setup: criar tag via evento e vincular a um comunicado
        consumer.handle(turmaEnvelope("evt-filter-int-01", turmaId.toString(), "MIDS-78"));
        Tag classTag = tagRepository.findByEntityTypeAndHubEntityId(TagEntityType.CLASS, turmaId.toString())
                .orElseThrow();

        Announcement linked = announcementRepository.save(publishedAnnouncement());
        Announcement unlinked = announcementRepository.save(publishedAnnouncement());

        autoLinkTagsByDestinationUseCase.execute(linked, List.of(
                new TagLinkDestinationCommand(AnnouncementDestinationType.CLASS, turmaId)
        ));

        // Filtrar por tagId (equivalente a GET /api/posts?tagId={classTag.id})
        var spec = AnnouncementSpecifications.notRemoved()
                .and(AnnouncementSpecifications.isPublished())
                .and(AnnouncementSpecifications.hasAnyTag(List.of(classTag.getId())));

        Page<Announcement> results = announcementRepository.findAll(spec, Pageable.unpaged());

        assertThat(results.getTotalElements()).isEqualTo(1);
        assertThat(results.getContent().get(0).getId()).isEqualTo(linked.getId());

        // comunicado sem vínculo não deve aparecer no filtro
        assertThat(results.getContent()).noneMatch(a -> a.getId().equals(unlinked.getId()));
    }

    private CoreEntityEventEnvelope turmaEnvelope(String eventId, String entityId, String name) {
        return new CoreEntityEventEnvelope(
                eventId, "corr-" + eventId, "core-api", "turma.created",
                Instant.parse("2026-06-19T18:00:00Z"), "turma", entityId, null, name
        );
    }

    private Announcement publishedAnnouncement() {
        Instant now = Instant.now();
        return Announcement.builder()
                .title("Comunicado de Integração")
                .description("Descrição do comunicado")
                .origin(AnnouncementOrigin.SENAI)
                .status(AnnouncementStatus.PUBLISHED)
                .pinned(false)
                .createdByUserId(UUID.randomUUID())
                .createdByUserType(UserType.SENAI)
                .publishedByUserId(UUID.randomUUID())
                .publishedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
