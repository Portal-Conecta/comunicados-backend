package com.portal.conecta.comunicados.module.comunicado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.portal.conecta.comunicados.module.comunicado.presentation.controller.AnnouncementController;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PublishAnnouncementFlowTest {

    private AnnouncementRepository announcementRepository;
    private AnnouncementHistoryRepository announcementHistoryRepository;
    private RequestContextProvider requestContextProvider;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        announcementRepository = mock(AnnouncementRepository.class);
        announcementHistoryRepository = mock(AnnouncementHistoryRepository.class);
        requestContextProvider = mock(RequestContextProvider.class);

        PublishAnnouncementUseCase publishAnnouncementUseCase = new PublishAnnouncementUseCase(
                announcementRepository,
                announcementHistoryRepository,
                requestContextProvider
        );


        AnnouncementController controller = new AnnouncementController(
                publishAnnouncementUseCase
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldPublishDraftAnnouncement() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncement(announcementId);
        announcement.setStatus(AnnouncementStatus.DRAFT);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));
        when(announcementRepository.save(announcement))
                .thenReturn(announcement);

        mockMvc.perform(patch("/api/posts/{id}/publish", announcementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(announcementId.toString()))
                .andExpect(jsonPath("$.status").value(AnnouncementStatus.PUBLISHED.name()))
                .andExpect(jsonPath("$.publishedByUserId").value(userId.toString()));

        assertEquals(AnnouncementStatus.PUBLISHED, announcement.getStatus());
        assertEquals(userId, announcement.getPublishedByUserId());
        assertNotNull(announcement.getPublishedAt());

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
    void shouldPublishScheduledAnnouncement() throws Exception {
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

        mockMvc.perform(patch("/api/posts/{id}/publish", announcementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(AnnouncementStatus.PUBLISHED.name()));

        verify(announcementRepository).save(announcement);
        verify(announcementHistoryRepository).save(any(AnnouncementHistory.class));
    }

    @Test
    void shouldReturnForbiddenForStudent() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncement(announcementId);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.STUDENT));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));

        mockMvc.perform(patch("/api/posts/{id}/publish", announcementId))
                .andExpect(status().isForbidden());

        verify(announcementRepository, never()).save(any());
        verify(announcementHistoryRepository, never()).save(any());
    }

    @Test
    void shouldPublishWhenTeacherHasLinkedClass() throws Exception {
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

        mockMvc.perform(patch("/api/posts/{id}/publish", announcementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(AnnouncementStatus.PUBLISHED.name()));

        verify(announcementRepository).save(announcement);
        verify(announcementHistoryRepository).save(any(AnnouncementHistory.class));
    }

    @Test
    void shouldReturnForbiddenWhenTeacherHasNoLinkedClass() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UUID announcementClassId = UUID.randomUUID();
        UUID teacherClassId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncementForClass(announcementId, announcementClassId);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.TEACHER, new ContextClass(teacherClassId, ClassRole.TEACHER)));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));

        mockMvc.perform(patch("/api/posts/{id}/publish", announcementId))
                .andExpect(status().isForbidden());

        verify(announcementRepository, never()).save(any());
        verify(announcementHistoryRepository, never()).save(any());
    }

    @Test
    void shouldReturnNotFoundWhenAnnouncementDoesNotExist() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/posts/{id}/publish", announcementId))
                .andExpect(status().isNotFound());

        verify(announcementRepository, never()).save(any());
        verify(announcementHistoryRepository, never()).save(any());
    }

    @Test
    void shouldReturnConflictWhenAnnouncementIsAlreadyPublished() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncement(announcementId);
        announcement.setStatus(AnnouncementStatus.PUBLISHED);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));

        mockMvc.perform(patch("/api/posts/{id}/publish", announcementId))
                .andExpect(status().isConflict());

        verify(announcementRepository, never()).save(any());
        verify(announcementHistoryRepository, never()).save(any());
    }

    @Test
    void shouldReturnBadRequestWhenAnnouncementIsIncomplete() throws Exception {
        assertBadRequestForAnnouncementWithoutTitle();
        assertBadRequestForAnnouncementWithoutDescription();
        assertBadRequestForAnnouncementWithoutDestination();
    }

    private void assertBadRequestForAnnouncementWithoutTitle() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncement(announcementId);
        announcement.setTitle("");

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));

        mockMvc.perform(patch("/api/posts/{id}/publish", announcementId))
                .andExpect(status().isBadRequest());
    }

    private void assertBadRequestForAnnouncementWithoutDescription() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncement(announcementId);
        announcement.setDescription("");

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));

        mockMvc.perform(patch("/api/posts/{id}/publish", announcementId))
                .andExpect(status().isBadRequest());
    }

    private void assertBadRequestForAnnouncementWithoutDestination() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncement(announcementId);
        announcement.setDestinations(List.of());

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));

        mockMvc.perform(patch("/api/posts/{id}/publish", announcementId))
                .andExpect(status().isBadRequest());
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