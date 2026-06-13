package com.portal.conecta.comunicados;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementTag;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.AnnouncementTagRepository;
import com.portal.conecta.comunicados.module.tag.application.dto.TagLinkDestinationCommand;
import com.portal.conecta.comunicados.module.tag.application.usecase.AutoLinkTagsByDestinationUseCase;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.domain.port.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AutoLinkTagsByDestinationUseCaseTest {

    private TagRepository tagRepository;
    private AnnouncementTagRepository announcementTagRepository;
    private AutoLinkTagsByDestinationUseCase useCase;

    private Announcement announcement;

    @BeforeEach
    void setUp() {
        tagRepository = mock(TagRepository.class);
        announcementTagRepository = mock(AnnouncementTagRepository.class);
        useCase = new AutoLinkTagsByDestinationUseCase(tagRepository, announcementTagRepository);

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
        when(tagRepository.findByEntityTypeAndHubEntityIdAndActiveTrue(TagEntityType.CLASS, classId))
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
        when(tagRepository.findByEntityTypeAndHubEntityIdAndActiveTrue(TagEntityType.COURSE, courseId))
                .thenReturn(Optional.of(tag));

        useCase.execute(announcement, List.of(command(AnnouncementDestinationType.COURSE, courseId)));

        ArgumentCaptor<List<AnnouncementTag>> captor = ArgumentCaptor.captor();
        verify(announcementTagRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    void shouldLinkGeneralTag() {
        Tag tag = activeTag(TagEntityType.GENERAL, null);
        when(tagRepository.findFirstByEntityTypeAndActiveTrueOrderByCreatedAtAsc(TagEntityType.GENERAL))
                .thenReturn(Optional.of(tag));

        useCase.execute(announcement, List.of(command(AnnouncementDestinationType.GENERAL, null)));

        verify(announcementTagRepository).saveAll(any());
    }

    @Test
    void shouldNotLinkUserDestination() {
        useCase.execute(announcement, List.of(command(AnnouncementDestinationType.USER, UUID.randomUUID())));

        verify(announcementTagRepository, never()).saveAll(any());
    }

    @Test
    void shouldSkipWhenTagNotFound() {
        UUID classId = UUID.randomUUID();
        when(tagRepository.findByEntityTypeAndHubEntityIdAndActiveTrue(TagEntityType.CLASS, classId))
                .thenReturn(Optional.empty());

        useCase.execute(announcement, List.of(command(AnnouncementDestinationType.CLASS, classId)));

        verify(announcementTagRepository, never()).saveAll(any());
    }

    @Test
    void shouldNotDuplicateAlreadyLinkedTag() {
        UUID classId = UUID.randomUUID();
        Tag tag = activeTag(TagEntityType.CLASS, classId);
        when(tagRepository.findByEntityTypeAndHubEntityIdAndActiveTrue(TagEntityType.CLASS, classId))
                .thenReturn(Optional.of(tag));

        AnnouncementTag existing = AnnouncementTag.builder().announcement(announcement).tag(tag).build();
        when(announcementTagRepository.findByAnnouncementId(announcement.getId())).thenReturn(List.of(existing));

        useCase.execute(announcement, List.of(command(AnnouncementDestinationType.CLASS, classId)));

        verify(announcementTagRepository, never()).saveAll(any());
    }

    @Test
    void shouldLinkMultipleDestinations() {
        UUID class1 = UUID.randomUUID();
        UUID class2 = UUID.randomUUID();
        Tag tag1 = activeTag(TagEntityType.CLASS, class1);
        Tag tag2 = activeTag(TagEntityType.CLASS, class2);

        when(tagRepository.findByEntityTypeAndHubEntityIdAndActiveTrue(TagEntityType.CLASS, class1))
                .thenReturn(Optional.of(tag1));
        when(tagRepository.findByEntityTypeAndHubEntityIdAndActiveTrue(TagEntityType.CLASS, class2))
                .thenReturn(Optional.of(tag2));

        useCase.execute(announcement, List.of(
                command(AnnouncementDestinationType.CLASS, class1),
                command(AnnouncementDestinationType.CLASS, class2)
        ));

        ArgumentCaptor<List<AnnouncementTag>> captor = ArgumentCaptor.captor();
        verify(announcementTagRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    private Tag activeTag(TagEntityType type, UUID hubEntityId) {
        return Tag.builder()
                .id(UUID.randomUUID())
                .name("Tag " + type)
                .entityType(type)
                .hubEntityId(hubEntityId != null ? hubEntityId : UUID.randomUUID())
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private TagLinkDestinationCommand command(AnnouncementDestinationType type, UUID hubEntityId) {
        return new TagLinkDestinationCommand(type, hubEntityId);
    }
}
