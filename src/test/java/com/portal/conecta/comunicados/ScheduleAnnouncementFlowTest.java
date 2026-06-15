package com.portal.conecta.comunicados.module.comunicado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.portal.conecta.comunicados.module.comunicado.application.command.ScheduleAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.DeleteAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.GetAnnouncementByIdUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.ListAnnouncementsUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.PublishAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.ScheduleAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementMustBeInTheFutureException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementDestinationRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubClassPort;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.module.comunicado.presentation.controller.AnnouncementController;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementDestinationInput;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

class ScheduleAnnouncementFlowTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private AnnouncementRepository announcementRepository;
    private AnnouncementDestinationRepository announcementDestinationRepository;
    private AnnouncementHistoryRepository announcementHistoryRepository;
    private RequestContextProvider requestContextProvider;
    private HubClassPort hubClassPort;
    private ListAnnouncementsUseCase listAnnouncementsUseCase;
    private GetAnnouncementByIdUseCase getAnnouncementByIdUseCase;
    private DeleteAnnouncementUseCase deleteAnnouncementUseCase;
    private PublishAnnouncementUseCase publishAnnouncementUseCase;
    private ScheduleAnnouncementUseCase scheduleAnnouncementUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        announcementRepository = mock(AnnouncementRepository.class);
        announcementDestinationRepository = mock(AnnouncementDestinationRepository.class);
        announcementHistoryRepository = mock(AnnouncementHistoryRepository.class);
        requestContextProvider = mock(RequestContextProvider.class);
        hubClassPort = mock(HubClassPort.class);
        listAnnouncementsUseCase = mock(ListAnnouncementsUseCase.class);
        getAnnouncementByIdUseCase = mock(GetAnnouncementByIdUseCase.class);
        deleteAnnouncementUseCase = mock(DeleteAnnouncementUseCase.class);
        publishAnnouncementUseCase = mock(PublishAnnouncementUseCase.class);

        scheduleAnnouncementUseCase = new ScheduleAnnouncementUseCase(
                announcementRepository,
                announcementDestinationRepository,
                announcementHistoryRepository,
                requestContextProvider,
                new AnnouncementPermissionValidator(hubClassPort)
        );

        AnnouncementController controller = new AnnouncementController(
                publishAnnouncementUseCase,
                scheduleAnnouncementUseCase,
                listAnnouncementsUseCase,
                getAnnouncementByIdUseCase,
                deleteAnnouncementUseCase,
                requestContextProvider
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        when(announcementRepository.save(any(Announcement.class))).thenAnswer(invocation -> {
            Announcement announcement = invocation.getArgument(0);
            if (announcement.getId() == null) {
                announcement.setId(UUID.randomUUID());
            }
            return announcement;
        });
    }

    @Test
    void shouldCreateAndScheduleAnnouncement() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        Instant scheduledFor = Instant.now().plus(1, ChronoUnit.DAYS);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));

        perform(scheduleRequest(scheduledFor, classDestination(classId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value(AnnouncementStatus.SCHEDULED.name()))
                .andExpect(jsonPath("$.scheduledFor").exists());

        ArgumentCaptor<Announcement> announcementCaptor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementRepository).save(announcementCaptor.capture());

        Announcement saved = announcementCaptor.getValue();
        assertEquals(AnnouncementStatus.SCHEDULED, saved.getStatus());
        assertEquals(userId, saved.getCreatedByUserId());
        assertNotNull(saved.getScheduledFor());

        verify(announcementDestinationRepository).saveAll(any());

        ArgumentCaptor<AnnouncementHistory> historyCaptor = ArgumentCaptor.forClass(AnnouncementHistory.class);
        verify(announcementHistoryRepository, times(2)).save(historyCaptor.capture());

        List<AnnouncementHistory> histories = historyCaptor.getAllValues();
        assertEquals(AnnouncementHistoryAction.CREATION, histories.get(0).getAction());
        assertEquals(AnnouncementHistoryAction.SCHEDULED, histories.get(1).getAction());
    }

    @Test
    void shouldReturnForbiddenForStudent() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant scheduledFor = Instant.now().plus(1, ChronoUnit.DAYS);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.STUDENT));

        perform(scheduleRequest(scheduledFor, classDestination(UUID.randomUUID())))
                .andExpect(status().isForbidden());

        verify(announcementRepository, never()).save(any());
    }

    @Test
    void shouldScheduleWhenTeacherOwnsClassDestination() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        Instant scheduledFor = Instant.now().plus(1, ChronoUnit.DAYS);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.TEACHER, new ContextClass(classId, ClassRole.TEACHER)));

        perform(scheduleRequest(scheduledFor, classDestination(classId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(AnnouncementStatus.SCHEDULED.name()));

        verify(announcementRepository).save(any());
    }

    @Test
    void shouldReturnForbiddenWhenRepresentativeHasDifferentClass() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID announcementClassId = UUID.randomUUID();
        UUID representativeClassId = UUID.randomUUID();
        Instant scheduledFor = Instant.now().plus(1, ChronoUnit.DAYS);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(
                        userId,
                        UserType.REPRESENTATIVE,
                        new ContextClass(representativeClassId, ClassRole.REPRESENTATIVE)
                ));

        perform(scheduleRequest(scheduledFor, classDestination(announcementClassId)))
                .andExpect(status().isForbidden());

        verify(announcementRepository, never()).save(any());
    }

    @Test
    void shouldReturnBadRequestWhenScheduledForIsInThePast() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant scheduledFor = Instant.now().minus(1, ChronoUnit.HOURS);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));

        perform(scheduleRequest(scheduledFor, classDestination(UUID.randomUUID())))
                .andExpect(status().isBadRequest());

        verify(announcementRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenScheduledForIsInThePastDirectlyInUseCase() {
        UUID userId = UUID.randomUUID();
        Instant past = Instant.now().minus(1, ChronoUnit.HOURS);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));

        ScheduleAnnouncementCommand command = new ScheduleAnnouncementCommand(
                "Titulo", "Descricao", AnnouncementOrigin.SENAI, false, past, userId,
                List.of(classDestination(UUID.randomUUID()))
        );

        assertThrows(AnnouncementMustBeInTheFutureException.class, () -> scheduleAnnouncementUseCase.execute(command));

        verify(announcementRepository, never()).save(any());
    }

    @Test
    void shouldReturnBadRequestWhenThereAreNoDestinations() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant scheduledFor = Instant.now().plus(1, ChronoUnit.DAYS);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));

        perform(scheduleRequest(scheduledFor))
                .andExpect(status().isBadRequest());

        verify(announcementRepository, never()).save(any());
    }

    private RequestContext createContext(UUID userId, UserType userType, ContextClass... classes) {
        return new RequestContext(userId, userType, List.of(classes));
    }

    private CreateAnnouncementDestinationInput classDestination(UUID referenceId) {
        return new CreateAnnouncementDestinationInput(AnnouncementDestinationType.CLASS, referenceId);
    }

    private ScheduleAnnouncementRequest scheduleRequest(Instant scheduledFor, CreateAnnouncementDestinationInput... destinations) {
        return new ScheduleAnnouncementRequest(
                "Comunicado de teste",
                "Descricao do comunicado",
                AnnouncementOrigin.SENAI,
                scheduledFor,
                List.of(destinations),
                null,
                null
        );
    }

    private ResultActions perform(ScheduleAnnouncementRequest request) throws Exception {
        return mockMvc.perform(post("/api/posts/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(request)));
    }
}

@WebMvcTest(AnnouncementController.class)
@AutoConfigureMockMvc(addFilters = false)
class ScheduleAnnouncementWebMvcTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublishAnnouncementUseCase publishAnnouncementUseCase;

    @MockitoBean
    private ScheduleAnnouncementUseCase scheduleAnnouncementUseCase;

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

        when(requestContextProvider.getRequestContext())
                .thenReturn(new RequestContext(userId, UserType.SENAI, List.of()));
        when(scheduleAnnouncementUseCase.execute(any()))
                .thenReturn(createScheduledAnnouncement(announcementId, userId, scheduledFor));

        mockMvc.perform(post("/api/posts/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(scheduledFor))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(announcementId.toString()))
                .andExpect(jsonPath("$.status").value(AnnouncementStatus.SCHEDULED.name()))
                .andExpect(jsonPath("$.scheduledFor").exists());
    }

    @Test
    void shouldReturnForbiddenByEndpoint() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant scheduledFor = Instant.now().plus(1, ChronoUnit.DAYS);

        when(requestContextProvider.getRequestContext())
                .thenReturn(new RequestContext(userId, UserType.TEACHER, List.of()));
        when(scheduleAnnouncementUseCase.execute(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário sem permissão!"));

        mockMvc.perform(post("/api/posts/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(scheduledFor))))
                .andExpect(status().isForbidden());
    }

    private ScheduleAnnouncementRequest validRequest(Instant scheduledFor) {
        return new ScheduleAnnouncementRequest(
                "Comunicado agendado",
                "Descricao",
                AnnouncementOrigin.SENAI,
                scheduledFor,
                List.of(new CreateAnnouncementDestinationInput(AnnouncementDestinationType.CLASS, UUID.randomUUID())),
                null,
                null
        );
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
