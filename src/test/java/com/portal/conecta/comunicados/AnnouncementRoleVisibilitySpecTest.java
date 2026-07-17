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

import com.portal.conecta.comunicados.module.comunicado.domain.AnnouncementRoleAudience;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementTag;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.specification.AnnouncementSpecifications;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.shared.context.UserType;

/**
 * Garante que tag ROLE restringe o mural: GENERAL + ROLE STUDENT não aparece para TEACHER.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AnnouncementRoleVisibilitySpecTest {

    @Autowired
    private AnnouncementRepository repository;

    @Autowired
    private TestEntityManager em;

    @Test
    void studentOnlyGeneral_isVisibleToStudent_butNotTeacher() {
        Instant now = Instant.now();
        Tag studentRole = persistRoleTag("STUDENT", "Alunos", now);

        Announcement studentOnly = persistPublished("Só alunos", now);
        persistGeneralDestination(studentOnly);
        persistAnnouncementTag(studentOnly, studentRole);

        Announcement broadcast = persistPublished("Geral sem role", now);
        persistGeneralDestination(broadcast);

        em.flush();
        em.clear();

        Specification<Announcement> asStudent = AnnouncementSpecifications.notRemoved()
                .and(AnnouncementSpecifications.isPublished())
                .and(AnnouncementSpecifications.visibleTo(
                        List.of(), List.of(), UUID.randomUUID(), List.of(),
                        AnnouncementRoleAudience.viewerRoleCodes(UserType.STUDENT)));

        Specification<Announcement> asTeacher = AnnouncementSpecifications.notRemoved()
                .and(AnnouncementSpecifications.isPublished())
                .and(AnnouncementSpecifications.visibleTo(
                        List.of(), List.of(), UUID.randomUUID(), List.of(),
                        AnnouncementRoleAudience.viewerRoleCodes(UserType.TEACHER)));

        Specification<Announcement> asRepresentative = AnnouncementSpecifications.notRemoved()
                .and(AnnouncementSpecifications.isPublished())
                .and(AnnouncementSpecifications.visibleTo(
                        List.of(), List.of(), UUID.randomUUID(), List.of(),
                        AnnouncementRoleAudience.viewerRoleCodes(UserType.REPRESENTATIVE)));

        assertThat(repository.findAll(asStudent))
                .extracting(Announcement::getId)
                .contains(studentOnly.getId(), broadcast.getId());

        assertThat(repository.findAll(asRepresentative))
                .extracting(Announcement::getId)
                .contains(studentOnly.getId(), broadcast.getId());

        assertThat(repository.findAll(asTeacher))
                .extracting(Announcement::getId)
                .contains(broadcast.getId())
                .doesNotContain(studentOnly.getId());
    }

    private Tag persistRoleTag(String hubEntityId, String name, Instant now) {
        return em.persistFlushFind(Tag.builder()
                .name(name)
                .entityType(TagEntityType.ROLE)
                .hubEntityId(hubEntityId)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private Announcement persistPublished(String title, Instant now) {
        return em.persistFlushFind(Announcement.builder()
                .title(title)
                .description(title)
                .descriptionPlain(title)
                .origin(AnnouncementOrigin.SENAI)
                .status(AnnouncementStatus.PUBLISHED)
                .pinned(false)
                .createdByUserId(UUID.randomUUID())
                .publishedByUserId(UUID.randomUUID())
                .publishedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private void persistGeneralDestination(Announcement announcement) {
        em.persist(AnnouncementDestination.builder()
                .announcement(announcement)
                .type(AnnouncementDestinationType.GENERAL)
                .referenceId(null)
                .build());
    }

    private void persistAnnouncementTag(Announcement announcement, Tag tag) {
        em.persist(AnnouncementTag.builder()
                .announcement(announcement)
                .tag(tag)
                .build());
    }
}
