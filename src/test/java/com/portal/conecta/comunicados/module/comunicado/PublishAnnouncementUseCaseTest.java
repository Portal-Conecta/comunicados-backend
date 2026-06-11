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
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.shared.context.ClassRole;
import com.portal.conecta.comunicados.shared.context.ContextClass;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import com.portal.conecta.comunicados.shared.context.UserType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PublishAnnouncementUseCaseTest {

    private AnnouncementRepository announcementRepository;
    private AnnouncementHistoryRepository announcementHistoryRepository;
    private RequestContextProvider requestContextProvider;
    private PublishAnnouncementUseCase useCase;

    @BeforeEach
    void setUp() {
        announcementRepository = mock(AnnouncementRepository.class);
        announcementHistoryRepository = mock(AnnouncementHistoryRepository.class);
        requestContextProvider = mock(RequestContextProvider.class);

        useCase = new PublishAnnouncementUseCase(
                announcementRepository,
                announcementHistoryRepository,
                requestContextProvider
        );
    }

    @Test
    void shouldPublishDraftAnnouncement() {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncement(announcementId);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));
        when(announcementRepository.save(announcement))
                .thenReturn(announcement);

        Announcement result = useCase.execute(PublishAnnouncementCommand.from(announcementId));

        assertEquals(AnnouncementStatus.PUBLISHED, result.getStatus());
        assertEquals(userId, result.getPublishedByUserId());
        assertNotNull(result.getPublishedAt());

        ArgumentCaptor<AnnouncementHistory> historyCaptor = ArgumentCaptor.forClass(AnnouncementHistory.class);
        verify(announcementHistoryRepository).save(historyCaptor.capture());

        AnnouncementHistory history = historyCaptor.getValue();

        assertEquals(announcement, history.getAnnouncement());
        assertEquals(userId, history.getUserId());
        assertEquals(AnnouncementHistoryAction.PUBLICATION, history.getAction());
        assertNotNull(history.getSnapshot());
        assertNotNull(history.getCreatedAt());
    }

    @Test
    void shouldPublishScheduledAnnouncement() {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncement(announcementId);
        announcement.setStatus(AnnouncementStatus.SCHEDULED);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.WEG));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));
        when(announcementRepository.save(announcement))
                .thenReturn(announcement);

        Announcement result = useCase.execute(PublishAnnouncementCommand.from(announcementId));

        assertEquals(AnnouncementStatus.PUBLISHED, result.getStatus());
        assertEquals(userId, result.getPublishedByUserId());

        verify(announcementRepository).save(announcement);
        verify(announcementHistoryRepository).save(any(AnnouncementHistory.class));
    }

    @Test
    void shouldReturnNotFoundWhenAnnouncementDoesNotExist() {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> useCase.execute(PublishAnnouncementCommand.from(announcementId))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(announcementRepository, never()).save(any());
        verify(announcementHistoryRepository, never()).save(any());
    }

    @Test
    void shouldReturnConflictWhenAnnouncementIsAlreadyPublished() {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncement(announcementId);
        announcement.setStatus(AnnouncementStatus.PUBLISHED);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> useCase.execute(PublishAnnouncementCommand.from(announcementId))
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(announcementRepository, never()).save(any());
        verify(announcementHistoryRepository, never()).save(any());
    }

    @Test
    void shouldReturnBadRequestWhenAnnouncementIsIncomplete() {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncement(announcementId);
        announcement.setDestinations(List.of());

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> useCase.execute(PublishAnnouncementCommand.from(announcementId))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(announcementRepository, never()).save(any());
        verify(announcementHistoryRepository, never()).save(any());
    }

    @Test
    void shouldReturnForbiddenForStudent() {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncement(announcementId);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.STUDENT));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> useCase.execute(PublishAnnouncementCommand.from(announcementId))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(announcementRepository, never()).save(any());
        verify(announcementHistoryRepository, never()).save(any());
    }

    @Test
    void shouldPublishWhenTeacherHasLinkedClass() {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncementForClass(announcementId, classId);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.TEACHER, new ContextClass(classId, ClassRole.TEACHER)));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));
        when(announcementRepository.save(announcement))
                .thenReturn(announcement);

        Announcement result = useCase.execute(PublishAnnouncementCommand.from(announcementId));

        assertEquals(AnnouncementStatus.PUBLISHED, result.getStatus());
        verify(announcementRepository).save(announcement);
        verify(announcementHistoryRepository).save(any(AnnouncementHistory.class));
    }

    @Test
    void shouldReturnForbiddenWhenTeacherHasDifferentClass() {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncementForClass(announcementId, UUID.randomUUID());

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.TEACHER, new ContextClass(UUID.randomUUID(), ClassRole.TEACHER)));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> useCase.execute(PublishAnnouncementCommand.from(announcementId))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(announcementRepository, never()).save(any());
        verify(announcementHistoryRepository, never()).save(any());
    }

    private RequestContext createContext(UUID userId, UserType userType, ContextClass... classes) {
        return new RequestContext(userId, userType, List.of(classes));
    }

    private Announcement createValidAnnouncement(UUID announcementId) {
        Announcement announcement = new Announcement();

        announcement.setId(announcementId);
        announcement.setTitle("Comunicado de teste");
        announcement.setDescription("Descrição do comunicado de teste");
        announcement.setOrigin(AnnouncementOrigin.SENAI);
        announcement.setStatus(AnnouncementStatus.DRAFT);
        announcement.setPinned(false);
        announcement.setDestinations(List.of(new AnnouncementDestination()));

        return announcement;
    }

    private Announcement createValidAnnouncementForClass(UUID announcementId, UUID classId) {
        AnnouncementDestination destination = new AnnouncementDestination();
        destination.setType(AnnouncementDestinationType.CLASS);
        destination.setReferenceId(classId);

        Announcement announcement = createValidAnnouncement(announcementId);
        announcement.setDestinations(List.of(destination));

        return announcement;
    }

}