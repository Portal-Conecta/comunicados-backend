package com.portal.conecta.comunicados;

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
import com.portal.conecta.comunicados.module.comunicado.presentation.controller.AnnouncementController;
import com.portal.conecta.comunicados.shared.context.ClassRole;
import com.portal.conecta.comunicados.shared.context.ContextClass;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import com.portal.conecta.comunicados.shared.context.UserType;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.CreateAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.DeleteAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.GetAnnouncementByIdUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.ListAnnouncementsUseCase;
import com.portal.conecta.comunicados.shared.security.error.SecurityErrorResponseWriter;
import com.portal.conecta.comunicados.shared.security.token.JwtExtractToken;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.UpdateAnnouncementUseCase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PublishAnnouncementFlowTest {

    private AnnouncementRepository announcementRepository;
    private AnnouncementHistoryRepository announcementHistoryRepository;
    private RequestContextProvider requestContextProvider;
    private CreateAnnouncementUseCase createAnnouncementUseCase;
    private ListAnnouncementsUseCase listAnnouncementsUseCase;
    private GetAnnouncementByIdUseCase getAnnouncementByIdUseCase;
    private DeleteAnnouncementUseCase deleteAnnouncementUseCase;
    private PublishAnnouncementUseCase publishAnnouncementUseCase;
    private UpdateAnnouncementUseCase updateAnnouncementUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        announcementRepository = mock(AnnouncementRepository.class);
        announcementHistoryRepository = mock(AnnouncementHistoryRepository.class);
        requestContextProvider = mock(RequestContextProvider.class);
        createAnnouncementUseCase = mock(CreateAnnouncementUseCase.class);
        listAnnouncementsUseCase = mock(ListAnnouncementsUseCase.class);
        getAnnouncementByIdUseCase = mock(GetAnnouncementByIdUseCase.class);
        deleteAnnouncementUseCase = mock(DeleteAnnouncementUseCase.class);
        updateAnnouncementUseCase = mock(UpdateAnnouncementUseCase.class);

        publishAnnouncementUseCase = new PublishAnnouncementUseCase(
                announcementRepository,
                announcementHistoryRepository,
                requestContextProvider
        );

        AnnouncementController controller = new AnnouncementController(
                publishAnnouncementUseCase,
                createAnnouncementUseCase,
                listAnnouncementsUseCase,
                getAnnouncementByIdUseCase,
                deleteAnnouncementUseCase,
                requestContextProvider,
                updateAnnouncementUseCase
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
    void shouldPublishDraftAnnouncementDirectlyInUseCase() {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncement(announcementId);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));
        when(announcementRepository.save(announcement))
                .thenReturn(announcement);

        Announcement result = publishAnnouncementUseCase.execute(PublishAnnouncementCommand.from(announcementId));

        assertEquals(AnnouncementStatus.PUBLISHED, result.getStatus());
        assertEquals(userId, result.getPublishedByUserId());
        assertNotNull(result.getPublishedAt());

        verify(announcementRepository).save(announcement);
        verify(announcementHistoryRepository).save(any(AnnouncementHistory.class));
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
    void shouldPublishWhenRepresentativeHasOwnClass() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncementForClass(announcementId, classId);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.REPRESENTATIVE, new ContextClass(classId, ClassRole.REPRESENTATIVE)));
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
    void shouldReturnForbiddenWhenRepresentativeHasDifferentClass() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UUID announcementClassId = UUID.randomUUID();
        UUID representativeClassId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncementForClass(announcementId, announcementClassId);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.REPRESENTATIVE, new ContextClass(representativeClassId, ClassRole.REPRESENTATIVE)));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));

        mockMvc.perform(patch("/api/posts/{id}/publish", announcementId))
                .andExpect(status().isForbidden());

        verify(announcementRepository, never()).save(any());
        verify(announcementHistoryRepository, never()).save(any());
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
    void shouldReturnForbiddenWhenTeacherHasNoClassesInContext() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncementForClass(announcementId, classId);

        when(requestContextProvider.getRequestContext())
                .thenReturn(new RequestContext(userId, UserType.TEACHER, null));
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
        announcement.setDescription("Descricao do comunicado de teste");
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

@WebMvcTest(AnnouncementController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublishAnnouncementWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublishAnnouncementUseCase publishAnnouncementUseCase;

    @MockitoBean
    private CreateAnnouncementUseCase createAnnouncementUseCase;

    @MockitoBean
    private ListAnnouncementsUseCase listAnnouncementsUseCase;

    @MockitoBean
    private GetAnnouncementByIdUseCase getAnnouncementByIdUseCase;

    @MockitoBean
    private DeleteAnnouncementUseCase deleteAnnouncementUseCase;

    @MockitoBean
    private RequestContextProvider requestContextProvider;

    @MockitoBean
    private JwtExtractToken jwtExtractToken;

    @MockitoBean
    private SecurityErrorResponseWriter securityErrorResponseWriter;

    @MockitoBean
    private UpdateAnnouncementUseCase updateAnnouncementUseCase;

    @Test
    void shouldPublishAnnouncementByEndpoint() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Announcement announcement = createPublishedAnnouncement(announcementId, userId);

        when(publishAnnouncementUseCase.execute(PublishAnnouncementCommand.from(announcementId)))
                .thenReturn(announcement);

        mockMvc.perform(patch("/api/posts/{id}/publish", announcementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(announcementId.toString()))
                .andExpect(jsonPath("$.status").value(AnnouncementStatus.PUBLISHED.name()))
                .andExpect(jsonPath("$.publishedByUserId").value(userId.toString()));
    }

    @Test
    void shouldReturnBadRequestByEndpoint() throws Exception {
        UUID announcementId = UUID.randomUUID();

        when(publishAnnouncementUseCase.execute(PublishAnnouncementCommand.from(announcementId)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados obrigatórios ausentes"));

        mockMvc.perform(patch("/api/posts/{id}/publish", announcementId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnForbiddenByEndpoint() throws Exception {
        UUID announcementId = UUID.randomUUID();

        when(publishAnnouncementUseCase.execute(PublishAnnouncementCommand.from(announcementId)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão"));

        mockMvc.perform(patch("/api/posts/{id}/publish", announcementId))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnNotFoundByEndpoint() throws Exception {
        UUID announcementId = UUID.randomUUID();

        when(publishAnnouncementUseCase.execute(PublishAnnouncementCommand.from(announcementId)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Comunicado não encontrado"));

        mockMvc.perform(patch("/api/posts/{id}/publish", announcementId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnConflictByEndpoint() throws Exception {
        UUID announcementId = UUID.randomUUID();

        when(publishAnnouncementUseCase.execute(PublishAnnouncementCommand.from(announcementId)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Status inválido para publicação"));

        mockMvc.perform(patch("/api/posts/{id}/publish", announcementId))
                .andExpect(status().isConflict());
    }

    private Announcement createPublishedAnnouncement(UUID announcementId, UUID userId) {
        Instant now = Instant.now();

        Announcement announcement = new Announcement();

        announcement.setId(announcementId);
        announcement.setTitle("Comunicado publicado");
        announcement.setDescription("Descricao do comunicado publicado");
        announcement.setOrigin(AnnouncementOrigin.SENAI);
        announcement.setStatus(AnnouncementStatus.PUBLISHED);
        announcement.setPinned(false);
        announcement.setCreatedByUserId(userId);
        announcement.setPublishedByUserId(userId);
        announcement.setPublishedAt(now);
        announcement.setCreatedAt(now);
        announcement.setUpdatedAt(now);

        return announcement;
    }

}