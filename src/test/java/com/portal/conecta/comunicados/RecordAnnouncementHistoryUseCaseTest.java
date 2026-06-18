package com.portal.conecta.comunicados;

import com.portal.conecta.comunicados.module.comunicado.application.usecase.RecordAnnouncementHistoryUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordAnnouncementHistoryUseCaseTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private AnnouncementHistoryRepository historyRepository;

    @InjectMocks
    private RecordAnnouncementHistoryUseCase useCase;

    @Test
    void shouldRecordAnnouncementHistoryWithSnapshot() {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String snapshot = "{\"title\":\"Anterior\"}";

        Announcement announcement = Announcement.builder()
                .id(announcementId)
                .build();

        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));
        when(historyRepository.save(any(AnnouncementHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AnnouncementHistory result = useCase.record(
                announcementId,
                AnnouncementHistoryAction.EDIT,
                userId,
                snapshot
        );

        ArgumentCaptor<AnnouncementHistory> historyCaptor =
                ArgumentCaptor.forClass(AnnouncementHistory.class);
        verify(historyRepository).save(historyCaptor.capture());

        AnnouncementHistory saved = historyCaptor.getValue();

        assertThat(result).isSameAs(saved);
        assertThat(saved.getAnnouncement()).isEqualTo(announcement);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getAction()).isEqualTo(AnnouncementHistoryAction.EDIT);
        assertThat(saved.getSnapshot()).isEqualTo(snapshot);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldThrowNotFound_WhenAnnouncementDoesNotExist() {
        UUID announcementId = UUID.randomUUID();

        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.record(
                announcementId,
                AnnouncementHistoryAction.PUBLICATION,
                UUID.randomUUID(),
                null
        )).isInstanceOf(AnnouncementNotFoundException.class);

        verify(historyRepository, never()).save(any());
    }
}