package com.portal.conecta.comunicados;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementTag;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.AnnouncementTagRepository;
import com.portal.conecta.comunicados.module.tag.application.dto.TagLinkDestinationCommand;
import com.portal.conecta.comunicados.module.tag.application.usecase.AutoLinkTagsByDestinationUseCase;
import com.portal.conecta.comunicados.module.tag.application.usecase.ResolveTagForDestinationUseCase;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.domain.port.TagRepository;

class AutoLinkTagsByDestinationUseCaseTest {

    private TagRepository tagRepository;
    private AnnouncementTagRepository announcementTagRepository;
    private ResolveTagForDestinationUseCase resolveTagForDestinationUseCase;
    private AutoLinkTagsByDestinationUseCase useCase;

    private Announcement announcement;

    @BeforeEach
    void setUp() {
        tagRepository = mock(TagRepository.class);
        announcementTagRepository = mock(AnnouncementTagRepository.class);
        resolveTagForDestinationUseCase = mock(ResolveTagForDestinationUseCase.class);
        useCase = new AutoLinkTagsByDestinationUseCase(
                tagRepository, announcementTagRepository, resolveTagForDestinationUseCase);

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
    void shouldLinkClassTag() {
        UUID classId = UUID.randomUUID();
        Tag tag = activeTag(TagEntityType.CLASS, classId);

        when(resolveTagForDestinationUseCase.resolve(TagEntityType.CLASS, classId.toString()))
                .thenReturn(Optional.of(tag));

        useCase.execute(announcement, List.of(command(AnnouncementDestinationType.CLASS, classId)));

        ArgumentCaptor<List<AnnouncementTag>> captor = ArgumentCaptor.captor();
        verify(announcementTagRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getTag()).isEqualTo(tag);
    }

    @Test
    void shouldLinkCourseTag() {
        UUID courseId = UUID.randomUUID();
        Tag tag = activeTag(TagEntityType.COURSE, courseId);

        when(resolveTagForDestinationUseCase.resolve(TagEntityType.COURSE, courseId.toString()))
                .thenReturn(Optional.of(tag));

        useCase.execute(announcement, List.of(command(AnnouncementDestinationType.COURSE, courseId)));

        ArgumentCaptor<List<AnnouncementTag>> captor = ArgumentCaptor.captor();
        verify(announcementTagRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getTag()).isEqualTo(tag);
    }

    @Test
    void shouldLinkGeneralTag() {
        Tag tag = activeTag(TagEntityType.GENERAL, null);

        when(tagRepository.findFirstByEntityTypeAndActiveTrueOrderByCreatedAtAsc(TagEntityType.GENERAL))
                .thenReturn(Optional.of(tag));

        useCase.execute(announcement, List.of(command(AnnouncementDestinationType.GENERAL, null)));

        ArgumentCaptor<List<AnnouncementTag>> captor = ArgumentCaptor.captor();
        verify(announcementTagRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getTag()).isEqualTo(tag);
    }

    @Test
    void shouldLinkUserTag() {
        UUID userId = UUID.randomUUID();
        Tag tag = activeTag(TagEntityType.USER, userId);

        when(tagRepository.findByEntityTypeAndHubEntityIdAndActiveTrue(
                TagEntityType.USER,
                userId.toString()
        )).thenReturn(Optional.of(tag));

        useCase.execute(announcement, List.of(command(AnnouncementDestinationType.USER, userId)));

        ArgumentCaptor<List<AnnouncementTag>> captor = ArgumentCaptor.captor();
        verify(announcementTagRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getTag()).isEqualTo(tag);
    }

    @Test
    void shouldSkipUserWhenTagNotFound() {
        UUID userId = UUID.randomUUID();

        when(tagRepository.findByEntityTypeAndHubEntityIdAndActiveTrue(
                TagEntityType.USER,
                userId.toString()
        )).thenReturn(Optional.empty());

        useCase.execute(announcement, List.of(command(AnnouncementDestinationType.USER, userId)));

        verify(announcementTagRepository, never()).saveAll(any());
    }

    @Test
    void shouldSkipWhenTagNotFound() {
        UUID classId = UUID.randomUUID();

        when(resolveTagForDestinationUseCase.resolve(TagEntityType.CLASS, classId.toString()))
                .thenReturn(Optional.empty());

        useCase.execute(announcement, List.of(command(AnnouncementDestinationType.CLASS, classId)));

        verify(announcementTagRepository, never()).saveAll(any());
    }

    @Test
    void shouldNotDuplicateAlreadyLinkedTag() {
        UUID classId = UUID.randomUUID();
        Tag tag = activeTag(TagEntityType.CLASS, classId);

        when(resolveTagForDestinationUseCase.resolve(TagEntityType.CLASS, classId.toString()))
                .thenReturn(Optional.of(tag));

        AnnouncementTag existing = AnnouncementTag.builder()
                .announcement(announcement)
                .tag(tag)
                .build();

        when(announcementTagRepository.findByAnnouncementId(announcement.getId()))
                .thenReturn(List.of(existing));

        useCase.execute(announcement, List.of(command(AnnouncementDestinationType.CLASS, classId)));

        verify(announcementTagRepository, never()).saveAll(any());
    }

    @Test
    void shouldLinkMultipleDestinations() {
        UUID class1 = UUID.randomUUID();
        UUID class2 = UUID.randomUUID();
        Tag tag1 = activeTag(TagEntityType.CLASS, class1);
        Tag tag2 = activeTag(TagEntityType.CLASS, class2);

        when(resolveTagForDestinationUseCase.resolve(TagEntityType.CLASS, class1.toString()))
                .thenReturn(Optional.of(tag1));

        when(resolveTagForDestinationUseCase.resolve(TagEntityType.CLASS, class2.toString()))
                .thenReturn(Optional.of(tag2));

        useCase.execute(announcement, List.of(
                command(AnnouncementDestinationType.CLASS, class1),
                command(AnnouncementDestinationType.CLASS, class2)
        ));

        ArgumentCaptor<List<AnnouncementTag>> captor = ArgumentCaptor.captor();
        verify(announcementTagRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue())
                .extracting(AnnouncementTag::getTag)
                .containsExactlyInAnyOrder(tag1, tag2);
    }

    @Test
    void shouldLinkExplicitTagIds() {
        Tag tag = activeTag(TagEntityType.COURSE, UUID.randomUUID());

        when(tagRepository.findAllById(List.of(tag.getId()))).thenReturn(List.of(tag));

        useCase.execute(announcement, List.of(), List.of(tag.getId()));

        ArgumentCaptor<List<AnnouncementTag>> captor = ArgumentCaptor.captor();
        verify(announcementTagRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getTag()).isEqualTo(tag);
    }

    @Test
    void shouldNotDuplicateWhenTagInBothDestinationAndExplicit() {
        UUID classId = UUID.randomUUID();
        Tag tag = activeTag(TagEntityType.CLASS, classId);

        when(resolveTagForDestinationUseCase.resolve(TagEntityType.CLASS, classId.toString()))
                .thenReturn(Optional.of(tag));
        when(tagRepository.findAllById(List.of(tag.getId()))).thenReturn(List.of(tag));

        useCase.execute(announcement,
                List.of(command(AnnouncementDestinationType.CLASS, classId)),
                List.of(tag.getId()));

        ArgumentCaptor<List<AnnouncementTag>> captor = ArgumentCaptor.captor();
        verify(announcementTagRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    void shouldIgnoreRoleAndShiftExplicitTagIds() {
        Tag roleTag = activeTag(TagEntityType.ROLE, null);
        roleTag.setHubEntityId("STUDENT");
        Tag courseTag = activeTag(TagEntityType.COURSE, UUID.randomUUID());

        when(tagRepository.findAllById(List.of(roleTag.getId(), courseTag.getId())))
                .thenReturn(List.of(roleTag, courseTag));

        useCase.execute(announcement, List.of(), List.of(roleTag.getId(), courseTag.getId()));

        ArgumentCaptor<List<AnnouncementTag>> captor = ArgumentCaptor.captor();
        verify(announcementTagRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getTag()).isEqualTo(courseTag);
    }

    private Tag activeTag(TagEntityType type, UUID hubEntityId) {
        return Tag.builder()
                .id(UUID.randomUUID())
                .name("Tag " + type)
                .entityType(type)
                .hubEntityId(hubEntityId != null ? hubEntityId.toString() : UUID.randomUUID().toString())
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private TagLinkDestinationCommand command(AnnouncementDestinationType type, UUID hubEntityId) {
        return new TagLinkDestinationCommand(type, hubEntityId);
    }
}