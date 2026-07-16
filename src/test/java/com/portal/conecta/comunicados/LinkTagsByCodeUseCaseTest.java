package com.portal.conecta.comunicados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementTag;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.AnnouncementTagRepository;
import com.portal.conecta.comunicados.module.tag.application.usecase.LinkTagsByCodeUseCase;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.exception.InvalidTagCodeException;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.domain.port.TagRepository;

class LinkTagsByCodeUseCaseTest {

    private TagRepository tagRepository;
    private AnnouncementTagRepository announcementTagRepository;
    private LinkTagsByCodeUseCase useCase;
    private Announcement announcement;

    @BeforeEach
    void setUp() {
        tagRepository = mock(TagRepository.class);
        announcementTagRepository = mock(AnnouncementTagRepository.class);
        useCase = new LinkTagsByCodeUseCase(tagRepository, announcementTagRepository);

        announcement = Announcement.builder()
                .id(UUID.randomUUID())
                .title("Test")
                .description("Desc")
                .origin(AnnouncementOrigin.SENAI)
                .status(AnnouncementStatus.PUBLISHED)
                .createdByUserId(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(announcementTagRepository.findByAnnouncementId(announcement.getId())).thenReturn(List.of());
    }

    @Test
    void shouldLinkShiftTag() {
        Tag tag = tag(TagEntityType.SHIFT, "FULL_AM_PM");

        when(tagRepository.findByEntityTypeAndHubEntityId(TagEntityType.SHIFT, "FULL_AM_PM"))
                .thenReturn(Optional.of(tag));

        useCase.execute(announcement, TagEntityType.SHIFT, List.of("FULL_AM_PM"));

        ArgumentCaptor<List<AnnouncementTag>> captor = ArgumentCaptor.captor();
        verify(announcementTagRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getTag()).isEqualTo(tag);
    }

    @Test
    void shouldLinkRoleTag() {
        Tag tag = tag(TagEntityType.ROLE, "TEACHER");

        when(tagRepository.findByEntityTypeAndHubEntityId(TagEntityType.ROLE, "TEACHER"))
                .thenReturn(Optional.of(tag));

        useCase.execute(announcement, TagEntityType.ROLE, List.of("TEACHER"));

        ArgumentCaptor<List<AnnouncementTag>> captor = ArgumentCaptor.captor();
        verify(announcementTagRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getTag()).isEqualTo(tag);
    }

    @Test
    void shouldCreateRoleTagWhenMissing() {
        when(tagRepository.findByEntityTypeAndHubEntityId(TagEntityType.ROLE, "STUDENT"))
                .thenReturn(Optional.empty());
        Tag created = tag(TagEntityType.ROLE, "STUDENT");
        when(tagRepository.save(any(Tag.class))).thenReturn(created);

        useCase.execute(announcement, TagEntityType.ROLE, List.of("STUDENT"));

        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(tagCaptor.capture());
        assertThat(tagCaptor.getValue().getEntityType()).isEqualTo(TagEntityType.ROLE);
        assertThat(tagCaptor.getValue().getHubEntityId()).isEqualTo("STUDENT");
        assertThat(tagCaptor.getValue().getName()).isEqualTo("Alunos");

        ArgumentCaptor<List<AnnouncementTag>> linkCaptor = ArgumentCaptor.captor();
        verify(announcementTagRepository).saveAll(linkCaptor.capture());
        assertThat(linkCaptor.getValue()).hasSize(1);
    }

    @Test
    void shouldReactivateInactiveRoleTag() {
        Tag inactive = tag(TagEntityType.ROLE, "STUDENT");
        inactive.setActive(false);
        when(tagRepository.findByEntityTypeAndHubEntityId(TagEntityType.ROLE, "STUDENT"))
                .thenReturn(Optional.of(inactive));
        when(tagRepository.save(inactive)).thenAnswer(invocation -> {
            inactive.setActive(true);
            return inactive;
        });

        useCase.execute(announcement, TagEntityType.ROLE, List.of("STUDENT"));

        verify(tagRepository).save(inactive);
        assertThat(inactive.isActive()).isTrue();
        verify(announcementTagRepository).saveAll(any());
    }

    @Test
    void shouldSkipWhenCodesEmpty() {
        useCase.execute(announcement, TagEntityType.SHIFT, List.of());

        verify(announcementTagRepository, never()).saveAll(any());
    }

    @Test
    void shouldSkipWhenCodesNull() {
        useCase.execute(announcement, TagEntityType.ROLE, null);

        verify(announcementTagRepository, never()).saveAll(any());
    }

    @Test
    void shouldThrowWhenNonLocalTagCodeMissing() {
        when(tagRepository.findByEntityTypeAndHubEntityId(TagEntityType.COURSE, "x"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(announcement, TagEntityType.COURSE, List.of("x")))
                .isInstanceOf(InvalidTagCodeException.class)
                .hasMessageContaining("COURSE");

        verify(announcementTagRepository, never()).saveAll(any());
    }

    private Tag tag(TagEntityType entityType, String code) {
        return Tag.builder()
                .id(UUID.randomUUID())
                .name(code)
                .entityType(entityType)
                .hubEntityId(code)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
