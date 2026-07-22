package com.portal.conecta.comunicados;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.specification.AnnouncementSpecifications;

/**
 * Autor sempre vê o próprio post no mural, mesmo fora da audiência (destino/turno/role) —
 * mesma regra já aplicada em {@code GetAnnouncementByIdUseCase.canAccess()} para o detalhe.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AnnouncementAuthorVisibilitySpecTest {

    @Autowired
    private AnnouncementRepository repository;

    @Autowired
    private TestEntityManager em;

    @Test
    void authorSeesOwnPost_evenWhenOutsideAudience() {
        Instant now = Instant.now();
        UUID authorId = UUID.randomUUID();
        UUID otherClassId = UUID.randomUUID();

        Announcement outsideAudience = persistPublished("Turma que não é minha", authorId, now);
        persistClassDestination(outsideAudience, otherClassId);

        Announcement fromOtherAuthor = persistPublished("De outro autor, também fora da minha turma", UUID.randomUUID(), now);
        persistClassDestination(fromOtherAuthor, otherClassId);

        em.flush();
        em.clear();

        // Viewer não tem nenhuma turma/curso/turno em comum com o destino — só bate por ser autor.
        Specification<Announcement> asAuthor = AnnouncementSpecifications.notRemoved()
                .and(AnnouncementSpecifications.isPublished())
                .and(AnnouncementSpecifications.visibleTo(
                        List.of(), List.of(), authorId, List.of(), List.of()));

        assertThat(repository.findAll(asAuthor))
                .extracting(Announcement::getId)
                .contains(outsideAudience.getId())
                .doesNotContain(fromOtherAuthor.getId());
    }

    private Announcement persistPublished(String title, UUID authorId, Instant now) {
        return em.persistFlushFind(Announcement.builder()
                .title(title)
                .description(title)
                .descriptionPlain(title)
                .origin(AnnouncementOrigin.SENAI)
                .status(AnnouncementStatus.PUBLISHED)
                .pinned(false)
                .createdByUserId(authorId)
                .publishedByUserId(authorId)
                .publishedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private void persistClassDestination(Announcement announcement, UUID classId) {
        em.persist(AnnouncementDestination.builder()
                .announcement(announcement)
                .type(AnnouncementDestinationType.CLASS)
                .referenceId(classId)
                .build());
    }
}
