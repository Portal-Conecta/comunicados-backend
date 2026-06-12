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

import com.portal.conecta.comunicados.module.comunicado.application.command.ScheduleAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.CreateAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.DeleteAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.GetAnnouncementByIdUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.ListAnnouncementsUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.PublishAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.ScheduleAnnouncementUseCase;
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
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.ScheduleAnnouncementRequest;
import com.portal.conecta.comunicados.shared.context.ClassRole;
import com.portal.conecta.comunicados.shared.context.ContextClass;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import com.portal.conecta.comunicados.shared.context.UserType;
import com.portal.conecta.comunicados.shared.exception.GlobalExceptionHandler;
import com.portal.conecta.comunicados.shared.security.error.SecurityErrorResponseWriter;
import com.portal.conecta.comunicados.shared.security.token.JwtExtractToken;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

class ScheduleAnnouncementFlowTest {

    private AnnouncementRepository announcementRepository;
    private AnnouncementHistoryRepository announcementHistoryRepository;
    private RequestContextProvider requestContextProvider;
    private CreateAnnouncementUseCase createAnnouncementUseCase;
    private ListAnnouncementsUseCase listAnnouncementsUseCase;
    private GetAnnouncementByIdUseCase getAnnouncementByIdUseCase;
    private DeleteAnnouncementUseCase deleteAnnouncementUseCase;
    private PublishAnnouncementUseCase publishAnnouncementUseCase;
    private ScheduleAnnouncementUseCase scheduleAnnouncementUseCase;
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
        publishAnnouncementUseCase = mock(PublishAnnouncementUseCase.class);

        scheduleAnnouncementUseCase = new ScheduleAnnouncementUseCase(
                announcementRepository,
                announcementHistoryRepository,
                requestContextProvider
        );

        AnnouncementController controller = new AnnouncementController(
                publishAnnouncementUseCase,
                scheduleAnnouncementUseCase,
                createAnnouncementUseCase,
                listAnnouncementsUseCase,
                getAnnouncementByIdUseCase,
                deleteAnnouncementUseCase,
                requestContextProvider
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldScheduleDraftAnnouncement() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant scheduledFor = Instant.now().plus(1, ChronoUnit.DAYS);

        Announcement announcement = createValidAnnouncement(announcementId);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));
        when(announcementRepository.save(announcement))
                .thenReturn(announcement);

        mockMvc.perform(patch("/api/posts/{id}/schedule", announcementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledFor\":\"" + scheduledFor + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(announcementId.toString()))
                .andExpect(jsonPath("$.status").value(AnnouncementStatus.SCHEDULED.name()))
                .andExpect(jsonPath("$.scheduledFor").exists());

        assertEquals(AnnouncementStatus.SCHEDULED, announcement.getStatus());
        assertEquals(scheduledFor, announcement.getScheduledFor());

        ArgumentCaptor<AnnouncementHistory> historyCaptor = ArgumentCaptor.forClass(AnnouncementHistory.class);
        verify(announcementHistoryRepository).save(historyCaptor.capture());

        AnnouncementHistory history = historyCaptor.getValue();

        assertEquals(announcement, history.getAnnouncement());
        assertEquals(userId, history.getUserId());
        assertEquals(AnnouncementHistoryAction.SCHEDULED, history.getAction());
        assertNotNull(history.getSnapshot());
        assertNotNull(history.getCreatedAt());
    }

    @Test
    void shouldScheduleDraftAnnouncementDirectlyInUseCase() {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant scheduledFor = Instant.now().plus(2, ChronoUnit.HOURS);

        Announcement announcement = createValidAnnouncement(announcementId);
        ScheduleAnnouncementRequest request = new ScheduleAnnouncementRequest(scheduledFor);
        ScheduleAnnouncementCommand command = ScheduleAnnouncementCommand.fromRequest(announcementId, request);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.WEG));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));
        when(announcementRepository.save(announcement))
                .thenReturn(announcement);

        Announcement result = scheduleAnnouncementUseCase.execute(command);

        assertEquals(AnnouncementStatus.SCHEDULED, result.getStatus());
        assertEquals(scheduledFor, result.getScheduledFor());

        verify(announcementRepository).save(announcement);
        verify(announcementHistoryRepository).save(any(AnnouncementHistory.class));
    }

    @Test
    void shouldReturnForbiddenForStudent() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant scheduledFor = Instant.now().plus(1, ChronoUnit.DAYS);

        Announcement announcement = createValidAnnouncement(announcementId);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.STUDENT));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));

        mockMvc.perform(patch("/api/posts/{id}/schedule", announcementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledFor\":\"" + scheduledFor + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuário não autorizado."));

        verify(announcementRepository, never()).save(any());
        verify(announcementHistoryRepository, never()).save(any());
    }

    @Test
    void shouldScheduleWhenTeacherHasLinkedClass() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        Instant scheduledFor = Instant.now().plus(1, ChronoUnit.DAYS);

        Announcement announcement = createValidAnnouncementForClass(announcementId, classId);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.TEACHER, new ContextClass(classId, ClassRole.TEACHER)));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));
        when(announcementRepository.save(announcement))
                .thenReturn(announcement);

        mockMvc.perform(patch("/api/posts/{id}/schedule", announcementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledFor\":\"" + scheduledFor + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(AnnouncementStatus.SCHEDULED.name()));

        verify(announcementRepository).save(announcement);
        verify(announcementHistoryRepository).save(any(AnnouncementHistory.class));
    }

    @Test
    void shouldReturnForbiddenWhenRepresentativeHasDifferentClass() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant scheduledFor = Instant.now().plus(1, ChronoUnit.DAYS);

        UUID announcementClassId = UUID.randomUUID();
        UUID representativeClassId = UUID.randomUUID();

        Announcement announcement = createValidAnnouncementForClass(announcementId, announcementClassId);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(
                        userId,
                        UserType.REPRESENTATIVE,
                        new ContextClass(representativeClassId, ClassRole.REPRESENTATIVE)
                ));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));

        mockMvc.perform(patch("/api/posts/{id}/schedule", announcementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledFor\":\"" + scheduledFor + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuário não autorizado."));

        verify(announcementRepository, never()).save(any());
        verify(announcementHistoryRepository, never()).save(any());
    }

    @Test
    void shouldReturnBadRequestWhenScheduledForIsInThePast() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant scheduledFor = Instant.now().minus(1, ChronoUnit.HOURS);

        Announcement announcement = createValidAnnouncement(announcementId);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));

        mockMvc.perform(patch("/api/posts/{id}/schedule", announcementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledFor\":\"" + scheduledFor + "\"}"))
                .andExpect(status().isBadRequest());

        verify(announcementRepository, never()).save(any());
        verify(announcementHistoryRepository, never()).save(any());
    }

    @Test
    void shouldReturnNotFoundWhenAnnouncementDoesNotExist() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant scheduledFor = Instant.now().plus(1, ChronoUnit.DAYS);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/posts/{id}/schedule", announcementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledFor\":\"" + scheduledFor + "\"}"))
                .andExpect(status().isNotFound());

        verify(announcementRepository, never()).save(any());
        verify(announcementHistoryRepository, never()).save(any());
    }

    @Test
    void shouldReturnConflictWhenAnnouncementIsAlreadyPublished() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant scheduledFor = Instant.now().plus(1, ChronoUnit.DAYS);

        Announcement announcement = createValidAnnouncement(announcementId);
        announcement.setStatus(AnnouncementStatus.PUBLISHED);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));

        mockMvc.perform(patch("/api/posts/{id}/schedule", announcementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledFor\":\"" + scheduledFor + "\"}"))
                .andExpect(status().isConflict());

        verify(announcementRepository, never()).save(any());
        verify(announcementHistoryRepository, never()).save(any());
    }

    @Test
    void shouldReturnBadRequestWhenAnnouncementIsIncomplete() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant scheduledFor = Instant.now().plus(1, ChronoUnit.DAYS);

        Announcement announcement = createValidAnnouncement(announcementId);
        announcement.setDestinations(List.of());

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));

        mockMvc.perform(patch("/api/posts/{id}/schedule", announcementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledFor\":\"" + scheduledFor + "\"}"))
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
class ScheduleAnnouncementWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublishAnnouncementUseCase publishAnnouncementUseCase;

    @MockitoBean
    private ScheduleAnnouncementUseCase scheduleAnnouncementUseCase;

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

    @Test
    void shouldScheduleAnnouncementByEndpoint() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant scheduledFor = Instant.now().plus(1, ChronoUnit.DAYS);

        Announcement announcement = createScheduledAnnouncement(announcementId, userId, scheduledFor);
        ScheduleAnnouncementRequest request = new ScheduleAnnouncementRequest(scheduledFor);
        ScheduleAnnouncementCommand command = ScheduleAnnouncementCommand.fromRequest(announcementId, request);

        when(scheduleAnnouncementUseCase.execute(command)).thenReturn(announcement);

        mockMvc.perform(patch("/api/posts/{id}/schedule", announcementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledFor\":\"" + scheduledFor + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(announcementId.toString()))
                .andExpect(jsonPath("$.status").value(AnnouncementStatus.SCHEDULED.name()))
                .andExpect(jsonPath("$.scheduledFor").exists());
    }

    @Test
    void shouldReturnForbiddenByEndpoint() throws Exception {
        UUID announcementId = UUID.randomUUID();
        Instant scheduledFor = Instant.now().plus(1, ChronoUnit.DAYS);

        when(scheduleAnnouncementUseCase.execute(any(ScheduleAnnouncementCommand.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário sem permissão para agendar este comunicado!"));

        mockMvc.perform(patch("/api/posts/{id}/schedule", announcementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledFor\":\"" + scheduledFor + "\"}"))
                .andExpect(status().isForbidden());
    }

    private Announcement createScheduledAnnouncement(UUID announcementId, UUID userId, Instant scheduledFor) {
        Instant now = Instant.now();

        Announcement announcement = new Announcement();

        announcement.setId(announcementId);
        announcement.setTitle("Comunicado agendado");
        announcement.setDescription("Descricao do comunicado agendado");
        announcement.setOrigin(AnnouncementOrigin.SENAI);
        announcement.setStatus(AnnouncementStatus.SCHEDULED);
        announcement.setPinned(false);
        announcement.setCreatedByUserId(userId);
        announcement.setScheduledFor(scheduledFor);
        announcement.setCreatedAt(now);
        announcement.setUpdatedAt(now);

        return announcement;
    }
}
