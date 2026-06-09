package com.portal.conecta.comunicados.module.comunicado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.portal.conecta.comunicados.module.comunicado.application.command.PublishAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.PublishAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PublishAnnouncementUseCaseTest {

    @Test
    void shouldPublishDraftAnnouncement() {
        AnnouncementRepository announcementRepository = mock(AnnouncementRepository.class);
        AnnouncementHistoryRepository historyRepository = mock(AnnouncementHistoryRepository.class);
        PublishAnnouncementUseCase useCase = new PublishAnnouncementUseCase(announcementRepository, historyRepository);

        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncement();
        announcement.setId(announcementId);

        PublishAnnouncementCommand command = new PublishAnnouncementCommand(announcementId, null, userId);

        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));
        when(announcementRepository.save(announcement))
                .thenReturn(announcement);

        Announcement result = useCase.execute(command);

        assertEquals(AnnouncementStatus.PUBLISHED, result.getStatus());
        assertEquals(userId, result.getPublishedByUserId());
        assertNotNull(result.getPublishedAt());

        ArgumentCaptor<AnnouncementHistory> historyCaptor = ArgumentCaptor.forClass(AnnouncementHistory.class);
        verify(historyRepository).save(historyCaptor.capture());

        AnnouncementHistory history = historyCaptor.getValue();

        assertEquals(announcement, history.getAnnouncement());
        assertEquals(userId, history.getUserId());
        assertEquals(AnnouncementHistoryAction.PUBLICATION, history.getAction());
        assertNotNull(history.getSnapshot());
        assertNotNull(history.getCreatedAt());
    }

    @Test
    void shouldReturnNotFoundWhenAnnouncementDoesNotExist() {
        AnnouncementRepository announcementRepository = mock(AnnouncementRepository.class);
        AnnouncementHistoryRepository historyRepository = mock(AnnouncementHistoryRepository.class);
        PublishAnnouncementUseCase useCase = new PublishAnnouncementUseCase(announcementRepository, historyRepository);

        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        PublishAnnouncementCommand command = new PublishAnnouncementCommand(announcementId, null, userId);

        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> useCase.execute(command)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(announcementRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void shouldReturnBadRequestWhenPublisherIsMissing() {
        AnnouncementRepository announcementRepository = mock(AnnouncementRepository.class);
        AnnouncementHistoryRepository historyRepository = mock(AnnouncementHistoryRepository.class);
        PublishAnnouncementUseCase useCase = new PublishAnnouncementUseCase(announcementRepository, historyRepository);

        UUID announcementId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncement();
        announcement.setId(announcementId);

        PublishAnnouncementCommand command = new PublishAnnouncementCommand(announcementId, null, null);

        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> useCase.execute(command)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(announcementRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void shouldReturnConflictWhenAnnouncementIsAlreadyPublished() {
        AnnouncementRepository announcementRepository = mock(AnnouncementRepository.class);
        AnnouncementHistoryRepository historyRepository = mock(AnnouncementHistoryRepository.class);
        PublishAnnouncementUseCase useCase = new PublishAnnouncementUseCase(announcementRepository, historyRepository);

        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncement();
        announcement.setId(announcementId);
        announcement.setStatus(AnnouncementStatus.PUBLISHED);

        PublishAnnouncementCommand command = new PublishAnnouncementCommand(announcementId, null, userId);

        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> useCase.execute(command)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(announcementRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void shouldReturnBadRequestWhenTitleIsMissing() {
        AnnouncementRepository announcementRepository = mock(AnnouncementRepository.class);
        AnnouncementHistoryRepository historyRepository = mock(AnnouncementHistoryRepository.class);
        PublishAnnouncementUseCase useCase = new PublishAnnouncementUseCase(announcementRepository, historyRepository);

        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncement();
        announcement.setId(announcementId);
        announcement.setTitle("");

        PublishAnnouncementCommand command = new PublishAnnouncementCommand(announcementId, null, userId);

        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> useCase.execute(command)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(announcementRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void shouldReturnBadRequestWhenDescriptionIsMissing() {
        AnnouncementRepository announcementRepository = mock(AnnouncementRepository.class);
        AnnouncementHistoryRepository historyRepository = mock(AnnouncementHistoryRepository.class);
        PublishAnnouncementUseCase useCase = new PublishAnnouncementUseCase(announcementRepository, historyRepository);

        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncement();
        announcement.setId(announcementId);
        announcement.setDescription("");

        PublishAnnouncementCommand command = new PublishAnnouncementCommand(announcementId, null, userId);

        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> useCase.execute(command)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(announcementRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void shouldReturnBadRequestWhenDestinationIsMissing() {
        AnnouncementRepository announcementRepository = mock(AnnouncementRepository.class);
        AnnouncementHistoryRepository historyRepository = mock(AnnouncementHistoryRepository.class);
        PublishAnnouncementUseCase useCase = new PublishAnnouncementUseCase(announcementRepository, historyRepository);

        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncement();
        announcement.setId(announcementId);
        announcement.setDestinations(List.of());

        PublishAnnouncementCommand command = new PublishAnnouncementCommand(announcementId, null, userId);

        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> useCase.execute(command)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(announcementRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    private Announcement createValidAnnouncement() {
        Announcement announcement = new Announcement();

        announcement.setTitle("Comunicado de teste");
        announcement.setDescription("Descrição do comunicado de teste");
        announcement.setOrigin(AnnouncementOrigin.SENAI);
        announcement.setStatus(AnnouncementStatus.DRAFT);
        announcement.setDestinations(List.of(new AnnouncementDestination()));

        return announcement;
    }
}