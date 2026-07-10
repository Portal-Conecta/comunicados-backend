package com.portal.conecta.comunicados;

import com.portal.conecta.comunicados.module.comunicado.application.query.GetAnnouncementByIdQuery;
import com.portal.conecta.comunicados.module.comunicado.application.query.ListAnnouncementHistoryQuery;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.GetAnnouncementByIdUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.ListAnnouncementHistoryUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.AnnouncementHistoryFilterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListAnnouncementHistoryUseCaseTest {

    @Mock
    private GetAnnouncementByIdUseCase getAnnouncementByIdUseCase;

    @Mock
    private AnnouncementHistoryRepository historyRepository;

    @InjectMocks
    private ListAnnouncementHistoryUseCase useCase;

    @Test
    void shouldReturnHistoryPage_WhenAnnouncementIsVisible() {
        UUID announcementId = UUID.randomUUID();
        UUID viewerUserId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        Announcement announcement = Announcement.builder()
                .id(announcementId)
                .build();

        AnnouncementHistory history = AnnouncementHistory.builder()
                .id(UUID.randomUUID())
                .announcement(announcement)
                .userId(actorUserId)
                .action(AnnouncementHistoryAction.EDIT)
                .snapshot("{\"title\":\"Antigo\"}")
                .createdAt(Instant.now())
                .build();

        when(getAnnouncementByIdUseCase.execute(any(GetAnnouncementByIdQuery.class)))
                .thenReturn(announcement);
        when(historyRepository.findByAnnouncementIdOrderByCreatedAtDesc(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(history)));

        ListAnnouncementHistoryQuery query = new ListAnnouncementHistoryQuery(
                announcementId,
                viewerUserId,
                new AnnouncementHistoryFilterRequest(2, 5)
        );

        var result = useCase.execute(query);

        assertThat(result.getContent()).containsExactly(history);

        ArgumentCaptor<GetAnnouncementByIdQuery> detailQueryCaptor =
                ArgumentCaptor.forClass(GetAnnouncementByIdQuery.class);
        verify(getAnnouncementByIdUseCase).execute(detailQueryCaptor.capture());

        assertThat(detailQueryCaptor.getValue().id()).isEqualTo(announcementId);
        assertThat(detailQueryCaptor.getValue().viewerUserId()).isEqualTo(viewerUserId);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(historyRepository).findByAnnouncementIdOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq(announcementId),
                pageableCaptor.capture()
        );

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void shouldThrowNotFound_WhenAnnouncementIsNotVisible() {
        UUID announcementId = UUID.randomUUID();
        UUID viewerUserId = UUID.randomUUID();

        when(getAnnouncementByIdUseCase.execute(any(GetAnnouncementByIdQuery.class)))
                .thenThrow(new AnnouncementNotFoundException());

        ListAnnouncementHistoryQuery query = new ListAnnouncementHistoryQuery(
                announcementId,
                viewerUserId,
                new AnnouncementHistoryFilterRequest(0, 20)
        );

        assertThatThrownBy(() -> useCase.execute(query))
                .isInstanceOf(AnnouncementNotFoundException.class);

        verifyNoInteractions(historyRepository);
    }
}