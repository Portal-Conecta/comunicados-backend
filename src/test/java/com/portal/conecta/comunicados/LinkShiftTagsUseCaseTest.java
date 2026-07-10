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

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.ShiftCode;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementTag;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.AnnouncementTagRepository;
import com.portal.conecta.comunicados.module.tag.application.usecase.LinkShiftTagsUseCase;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.domain.port.TagRepository;

class LinkShiftTagsUseCaseTest {

    private TagRepository tagRepository;
    private AnnouncementTagRepository announcementTagRepository;
    private LinkShiftTagsUseCase useCase;
    private Announcement announcement;

    @BeforeEach
    void setUp() {
        tagRepository = mock(TagRepository.class);
        announcementTagRepository = mock(AnnouncementTagRepository.class);
        useCase = new LinkShiftTagsUseCase(tagRepository, announcementTagRepository);

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
        Tag tag = shiftTag("FULL_AM_PM");

        when(tagRepository.findByEntityTypeAndHubEntityIdAndActiveTrue(TagEntityType.SHIFT, "FULL_AM_PM"))
                .thenReturn(Optional.of(tag));

        useCase.execute(announcement, List.of(ShiftCode.FULL_AM_PM));

        ArgumentCaptor<List<AnnouncementTag>> captor = ArgumentCaptor.captor();
        verify(announcementTagRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getTag()).isEqualTo(tag);
    }

    @Test
    void shouldSkipWhenShiftCodesEmpty() {
        useCase.execute(announcement, List.of());

        verify(announcementTagRepository, never()).saveAll(any());
    }

    private Tag shiftTag(String code) {
        return Tag.builder()
                .id(UUID.randomUUID())
                .name("Turno")
                .entityType(TagEntityType.SHIFT)
                .hubEntityId(code)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
