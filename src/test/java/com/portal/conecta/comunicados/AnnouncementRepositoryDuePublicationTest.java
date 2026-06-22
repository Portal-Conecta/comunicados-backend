package com.portal.conecta.comunicados;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AnnouncementRepositoryDuePublicationTest {

    @Autowired
    private AnnouncementRepository repository;

    @Autowired
    private TestEntityManager em;

    private Announcement persist(AnnouncementStatus status, Instant scheduledFor, Instant removedAt) {
        Instant now = Instant.now();
        Announcement a = Announcement.builder()
                .title("Aviso")
                .description("Descrição")
                .origin(AnnouncementOrigin.BOTH)
                .status(status)
                .pinned(false)
                .createdByUserId(UUID.randomUUID())
                .scheduledFor(scheduledFor)
                .removedAt(removedAt)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return em.persistFlushFind(a);
    }

    @Test
    void findDueForPublication_returnsOnlyScheduledPastNotRemoved() {
        Instant past = Instant.now().minus(5, ChronoUnit.MINUTES);
        Instant future = Instant.now().plus(1, ChronoUnit.HOURS);

        Announcement due = persist(AnnouncementStatus.SCHEDULED, past, null);
        persist(AnnouncementStatus.SCHEDULED, future, null);        // ainda não venceu
        persist(AnnouncementStatus.PUBLISHED, past, null);          // já publicado
        persist(AnnouncementStatus.SCHEDULED, past, Instant.now()); // removido

        List<Announcement> result = repository.findDueForPublication(Instant.now());

        assertThat(result).extracting(Announcement::getId).containsExactly(due.getId());
    }

    @Test
    void markScheduledAsPublished_flipsRowOnceAndIsIdempotent() {
        Instant past = Instant.now().minus(5, ChronoUnit.MINUTES);
        Announcement due = persist(AnnouncementStatus.SCHEDULED, past, null);
        UUID author = due.getCreatedByUserId();
        Instant now = Instant.now();

        int first = repository.markScheduledAsPublished(due.getId(), now);
        int second = repository.markScheduledAsPublished(due.getId(), now);

        assertThat(first).isEqualTo(1);  // esta execução publicou
        assertThat(second).isZero();     // idempotente: já não está mais SCHEDULED

        em.flush();
        em.clear();
        Announcement reloaded = repository.findById(due.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AnnouncementStatus.PUBLISHED);
        assertThat(reloaded.getPublishedAt()).isNotNull();
        assertThat(reloaded.getPublishedByUserId()).isEqualTo(author);
    }
}
