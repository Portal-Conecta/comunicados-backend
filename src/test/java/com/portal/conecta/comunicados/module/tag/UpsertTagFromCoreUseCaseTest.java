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

import com.portal.conecta.comunicados.module.tag.application.command.UpsertTagFromCoreCommand;
import com.portal.conecta.comunicados.module.tag.application.usecase.UpsertTagFromCoreUseCase;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.domain.port.TagRepository;

@ExtendWith(MockitoExtension.class)
class UpsertTagFromCoreUseCaseTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private UpsertTagFromCoreUseCase useCase;

    @Test
    void shouldCreateTagWhenNotExists() {
        UUID hubEntityId = UUID.randomUUID();
        UpsertTagFromCoreCommand command = new UpsertTagFromCoreCommand(
                hubEntityId,
                TagEntityType.CLASS,
                "MI78 - Manhã",
                true
        );

        when(tagRepository.findByEntityTypeAndHubEntityId(TagEntityType.CLASS, hubEntityId))
                .thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tag result = useCase.execute(command);

        assertThat(result.getHubEntityId()).isEqualTo(hubEntityId);
        assertThat(result.getEntityType()).isEqualTo(TagEntityType.CLASS);
        assertThat(result.getName()).isEqualTo("MI78 - Manhã");
        assertThat(result.isActive()).isTrue();
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldUpdateTagWhenAlreadyExists() {
        UUID hubEntityId = UUID.randomUUID();
        Tag existing = Tag.builder()
                .id(UUID.randomUUID())
                .hubEntityId(hubEntityId)
                .entityType(TagEntityType.CLASS)
                .name("Nome antigo")
                .active(true)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();

        UpsertTagFromCoreCommand command = new UpsertTagFromCoreCommand(
                hubEntityId,
                TagEntityType.CLASS,
                "MI78 - Tarde",
                false
        );

        when(tagRepository.findByEntityTypeAndHubEntityId(TagEntityType.CLASS, hubEntityId))
                .thenReturn(Optional.of(existing));
        when(tagRepository.save(existing)).thenReturn(existing);

        Tag result = useCase.execute(command);

        assertThat(result.getName()).isEqualTo("MI78 - Tarde");
        assertThat(result.isActive()).isFalse();
        assertThat(result.getUpdatedAt()).isAfter(Instant.parse("2026-01-01T00:00:00Z"));
        verify(tagRepository).save(existing);
    }
}
