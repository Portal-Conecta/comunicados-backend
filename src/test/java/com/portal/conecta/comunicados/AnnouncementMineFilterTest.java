package com.portal.conecta.comunicados;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.specification.AnnouncementSpecifications;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova, contra banco real, que o escopo de "Meus Comunicados" (createdBy + notRemoved)
 * traz apenas os comunicados do próprio autor — incluindo os agendados (SCHEDULED) e
 * excluindo os removidos e os de outros autores.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AnnouncementMineFilterTest {

    @Autowired
    private AnnouncementRepository repository;

    @Autowired
    private TestEntityManager em;

    private Announcement persist(UUID author, AnnouncementStatus status, Instant publishedAt, Instant removedAt) {
        Instant now = Instant.now();
        Announcement a = Announcement.builder()
                .title("Aviso")
                .description("Descrição")
                .descriptionPlain("Descrição")
                .origin(AnnouncementOrigin.BOTH)
                .status(status)
                .pinned(false)
                .createdByUserId(author)
                .publishedAt(publishedAt)
                .removedAt(removedAt)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return em.persistFlushFind(a);
    }

    @Test
    void mineScope_returnsOnlyMyPublishedAndScheduled_excludingRemovedAndOthers() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Instant now = Instant.now();

        Announcement myPublished = persist(me, AnnouncementStatus.PUBLISHED, now, null);
        Announcement myScheduled = persist(me, AnnouncementStatus.SCHEDULED, null, null);
        persist(me, AnnouncementStatus.REMOVED, now, now);        // meu, mas removido → fora
        persist(other, AnnouncementStatus.PUBLISHED, now, null);  // publicado, mas de outro autor → fora

        Specification<Announcement> mineScope =
                AnnouncementSpecifications.notRemoved().and(AnnouncementSpecifications.createdBy(me));

        List<Announcement> result = repository.findAll(mineScope);

        assertThat(result)
                .extracting(Announcement::getId)
                .containsExactlyInAnyOrder(myPublished.getId(), myScheduled.getId());
    }

    @Test
    void createdBy_isNoOp_whenUserIdIsNull() {
        persist(UUID.randomUUID(), AnnouncementStatus.PUBLISHED, Instant.now(), null);
        persist(UUID.randomUUID(), AnnouncementStatus.PUBLISHED, Instant.now(), null);

        // H2 mem compartilhado entre @DataJpaTest: createdBy(null) pode trazer mais linhas.
        // Contamos só que a spec não restringe (sem toString circular em falha AssertJ).
        List<Announcement> result = repository.findAll(AnnouncementSpecifications.createdBy(null));

        assertThat(result.size()).isGreaterThanOrEqualTo(2);
    }
}
