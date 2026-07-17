package com.portal.conecta.comunicados;

import com.portal.conecta.comunicados.module.comunicado.application.service.AnnouncementNotificationDispatcher;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.AnnouncementPublisher;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementDestinationRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.AnnouncementTagRepository;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementPublisherTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private AnnouncementHistoryRepository announcementHistoryRepository;

    @Mock
    private AnnouncementDestinationRepository announcementDestinationRepository;

    @Mock
    private AnnouncementTagRepository announcementTagRepository;

    @Mock
    private AnnouncementNotificationDispatcher notificationDispatcher;

    @InjectMocks
    private AnnouncementPublisher publisher;

    private Announcement scheduled(UUID id, UUID author) {
        return Announcement.builder()
                .id(id)
                .title("Aviso de prova")
                .createdByUserId(author)
                .build();
    }

    @Test
    void shouldPublishAndRecordHistory_WhenCasWins() {
        UUID id = UUID.randomUUID();
        UUID author = UUID.randomUUID();
        Instant now = Instant.now();
        Announcement announcement = scheduled(id, author);

        when(announcementRepository.markScheduledAsPublished(eq(id), eq(now))).thenReturn(1);
        when(announcementRepository.getReferenceById(id)).thenReturn(announcement);
        when(announcementDestinationRepository.findByAnnouncementId(id)).thenReturn(List.of());
        when(announcementTagRepository.findActiveHubEntityIdsByAnnouncementIdAndEntityType(id, TagEntityType.ROLE))
                .thenReturn(List.of());

        boolean result = publisher.publish(announcement, now);

        assertThat(result).isTrue();

        ArgumentCaptor<AnnouncementHistory> captor = ArgumentCaptor.forClass(AnnouncementHistory.class);
        verify(announcementHistoryRepository).save(captor.capture());

        AnnouncementHistory history = captor.getValue();
        assertThat(history.getAction()).isEqualTo(AnnouncementHistoryAction.PUBLICATION);
        assertThat(history.getUserId()).isEqualTo(author);
        assertThat(history.getCreatedAt()).isEqualTo(now);
        assertThat(history.getSnapshot()).contains("Aviso de prova");

        verify(announcementDestinationRepository).findByAnnouncementId(id);
        verify(notificationDispatcher).dispatchAfterCommit(eq(announcement), any(List.class), any(List.class));
    }

    @Test
    void shouldNotRecordHistory_WhenCasLoses() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Announcement announcement = scheduled(id, UUID.randomUUID());

        when(announcementRepository.markScheduledAsPublished(eq(id), eq(now))).thenReturn(0);

        boolean result = publisher.publish(announcement, now);

        assertThat(result).isFalse();
        verify(announcementHistoryRepository, never()).save(any());
        verify(announcementRepository, never()).getReferenceById(any());
        verify(announcementDestinationRepository, never()).findByAnnouncementId(any());
        verify(notificationDispatcher, never()).dispatchAfterCommit(any(), any(), any());
    }
}
