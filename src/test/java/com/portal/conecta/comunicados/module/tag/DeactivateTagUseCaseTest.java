package com.portal.conecta.comunicados.module.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.portal.conecta.comunicados.module.tag.application.command.DeactivateTagCommand;
import com.portal.conecta.comunicados.module.tag.application.usecase.DeactivateTagUseCase;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.domain.port.TagRepository;

@ExtendWith(MockitoExtension.class)
class DeactivateTagUseCaseTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private DeactivateTagUseCase useCase;

    @Test
    void shouldDeactivateExistingTag() {
        String hubEntityId = "user-123";
        Tag tag = Tag.builder()
                .id(UUID.randomUUID())
                .hubEntityId(hubEntityId)
                .entityType(TagEntityType.USER)
                .name("Aluno X")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(tagRepository.findByEntityTypeAndHubEntityId(TagEntityType.USER, hubEntityId))
                .thenReturn(Optional.of(tag));
        when(tagRepository.save(tag)).thenReturn(tag);

        useCase.execute(new DeactivateTagCommand(hubEntityId, TagEntityType.USER));

        verify(tagRepository).save(tag);
        assertThat(tag.isActive()).isFalse();
    }

    @Test
    void shouldDoNothingWhenTagDoesNotExist() {
        String hubEntityId = "course-123";

        when(tagRepository.findByEntityTypeAndHubEntityId(TagEntityType.COURSE, hubEntityId))
                .thenReturn(Optional.empty());

        useCase.execute(new DeactivateTagCommand(hubEntityId, TagEntityType.COURSE));

        verify(tagRepository, never()).save(any());
    }
}