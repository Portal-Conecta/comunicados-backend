package com.portal.conecta.comunicados;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.DeleteAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.GetAnnouncementByIdUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.ListAnnouncementFilesUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.ListAnnouncementHistoryUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.ListAnnouncementsUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.ListPinnedAnnouncementsUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.PinAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.PublishAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.RescheduleAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.ScheduleAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.UnpinAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.UpdateAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.ShiftCode;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.application.service.AnnouncementNotificationDispatcher;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementDestinationRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubClassPort;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.module.comunicado.presentation.controller.AnnouncementController;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementDestinationInput;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.PublishAnnouncementRequest;
import com.portal.conecta.comunicados.module.tag.application.usecase.AutoLinkTagsByDestinationUseCase;
import com.portal.conecta.comunicados.module.tag.application.usecase.LinkTagsByCodeUseCase;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.shared.context.ClassRole;
import com.portal.conecta.comunicados.shared.context.ContextClass;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import com.portal.conecta.comunicados.shared.context.UserType;
import com.portal.conecta.comunicados.shared.exception.GlobalExceptionHandler;
import com.portal.conecta.comunicados.shared.security.error.SecurityErrorResponseWriter;
import com.portal.conecta.comunicados.shared.security.token.JwtExtractToken;

class PublishAnnouncementFlowTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AnnouncementRepository announcementRepository;
    private AnnouncementDestinationRepository announcementDestinationRepository;
    private AnnouncementHistoryRepository announcementHistoryRepository;
    private RequestContextProvider requestContextProvider;
    private HubClassPort hubClassPort;
    private AutoLinkTagsByDestinationUseCase autoLinkTagsUseCase;
    private LinkTagsByCodeUseCase linkTagsByCodeUseCase;
    private AnnouncementNotificationDispatcher notificationDispatcher;
    private ListAnnouncementsUseCase listAnnouncementsUseCase;
    private ListPinnedAnnouncementsUseCase listPinnedAnnouncementsUseCase;
    private GetAnnouncementByIdUseCase getAnnouncementByIdUseCase;
    private DeleteAnnouncementUseCase deleteAnnouncementUseCase;
    private PublishAnnouncementUseCase publishAnnouncementUseCase;
    private ScheduleAnnouncementUseCase scheduleAnnouncementUseCase;
    private PinAnnouncementUseCase pinAnnouncementUseCase;
    private UnpinAnnouncementUseCase unpinAnnouncementUseCase;
    private UpdateAnnouncementUseCase updateAnnouncementUseCase;
    private ListAnnouncementHistoryUseCase listAnnouncementHistoryUseCase;
    private RescheduleAnnouncementUseCase rescheduleAnnouncementUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        announcementRepository = mock(AnnouncementRepository.class);
        announcementDestinationRepository = mock(AnnouncementDestinationRepository.class);
        announcementHistoryRepository = mock(AnnouncementHistoryRepository.class);
        requestContextProvider = mock(RequestContextProvider.class);
        hubClassPort = mock(HubClassPort.class);
        autoLinkTagsUseCase = mock(AutoLinkTagsByDestinationUseCase.class);
        linkTagsByCodeUseCase = mock(LinkTagsByCodeUseCase.class);
        notificationDispatcher = mock(AnnouncementNotificationDispatcher.class);
        listAnnouncementsUseCase = mock(ListAnnouncementsUseCase.class);
        listPinnedAnnouncementsUseCase = mock(ListPinnedAnnouncementsUseCase.class);
        getAnnouncementByIdUseCase = mock(GetAnnouncementByIdUseCase.class);
        deleteAnnouncementUseCase = mock(DeleteAnnouncementUseCase.class);
        updateAnnouncementUseCase = mock(UpdateAnnouncementUseCase.class);
        listAnnouncementHistoryUseCase = mock(ListAnnouncementHistoryUseCase.class);
        rescheduleAnnouncementUseCase = mock(RescheduleAnnouncementUseCase.class);

        publishAnnouncementUseCase = new PublishAnnouncementUseCase(
                announcementRepository,
                announcementDestinationRepository,
                announcementHistoryRepository,
                requestContextProvider,
                new AnnouncementPermissionValidator(hubClassPort),
                new com.portal.conecta.comunicados.module.comunicado.domain.service.AnnouncementDescriptionNormalizer(),
                autoLinkTagsUseCase,
                linkTagsByCodeUseCase,
                notificationDispatcher
        );
        scheduleAnnouncementUseCase = mock(ScheduleAnnouncementUseCase.class);
        pinAnnouncementUseCase = mock(PinAnnouncementUseCase.class);
        unpinAnnouncementUseCase = mock(UnpinAnnouncementUseCase.class);

        AnnouncementController controller = new AnnouncementController(
                publishAnnouncementUseCase,
                scheduleAnnouncementUseCase,
                listAnnouncementsUseCase,
                listPinnedAnnouncementsUseCase,
                getAnnouncementByIdUseCase,
                mock(com.portal.conecta.comunicados.module.comunicado.application.usecase.ListAnnouncementFilesUseCase.class),
                deleteAnnouncementUseCase,
                requestContextProvider,
                pinAnnouncementUseCase,
                unpinAnnouncementUseCase,
                updateAnnouncementUseCase,
                listAnnouncementHistoryUseCase,
                rescheduleAnnouncementUseCase
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
    void shouldCreateAndPublishAnnouncement() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));

        perform(publishRequest(userDestination(studentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value(AnnouncementStatus.PUBLISHED.name()))
                .andExpect(jsonPath("$.publishedByUserId").value(userId.toString()));

        ArgumentCaptor<Announcement> announcementCaptor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementRepository).save(announcementCaptor.capture());

        Announcement saved = announcementCaptor.getValue();
        assertEquals(AnnouncementStatus.PUBLISHED, saved.getStatus());
        assertEquals(userId, saved.getCreatedByUserId());
        assertEquals(userId, saved.getPublishedByUserId());
        assertNotNull(saved.getPublishedAt());

        verify(announcementDestinationRepository).saveAll(any());
        verify(autoLinkTagsUseCase).execute(any(), any(), any());

        ArgumentCaptor<AnnouncementHistory> historyCaptor = ArgumentCaptor.forClass(AnnouncementHistory.class);
        verify(announcementHistoryRepository, times(2)).save(historyCaptor.capture());

        List<AnnouncementHistory> histories = historyCaptor.getAllValues();
        assertEquals(AnnouncementHistoryAction.CREATION, histories.get(0).getAction());
        assertEquals(AnnouncementHistoryAction.PUBLICATION, histories.get(1).getAction());
    }

    @Test
    void shouldSanitizeHtmlAndDeriveDescriptionPlainOnPublish() throws Exception {
        UUID userId = UUID.randomUUID();

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));

        PublishAnnouncementRequest request = new PublishAnnouncementRequest(
                "Comunicado HTML",
                "<p>Olá <strong>mundo</strong><script>alert(1)</script></p>",
                AnnouncementOrigin.SENAI,
                List.of(userDestination(UUID.randomUUID())),
                null,
                null,
                null,
                null
        );

        perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(org.hamcrest.Matchers.containsString("<strong>")))
                .andExpect(jsonPath("$.description").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("script"))))
                .andExpect(jsonPath("$.descriptionPlain").value("Olá mundo"));

        ArgumentCaptor<Announcement> announcementCaptor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementRepository).save(announcementCaptor.capture());
        Announcement saved = announcementCaptor.getValue();
        assertEquals("Olá mundo", saved.getDescriptionPlain());
        assertFalse(saved.getDescription().contains("script"));
    }

    @Test
    void shouldPublishToMultipleUserDestinations() throws Exception {
        UUID userId = UUID.randomUUID();

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.WEG));

        perform(publishRequest(userDestination(UUID.randomUUID()), userDestination(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(AnnouncementStatus.PUBLISHED.name()));

        verify(announcementRepository).save(any());
        verify(announcementDestinationRepository).saveAll(any());
    }

    @Test
    void shouldReturnForbiddenForStudent() throws Exception {
        UUID userId = UUID.randomUUID();

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.STUDENT));

        perform(publishRequest(userDestination(UUID.randomUUID())))
                .andExpect(status().isForbidden());

        verify(announcementRepository, never()).save(any());
        verify(announcementHistoryRepository, never()).save(any());
    }

    @Test
    void shouldPublishWhenTeacherOwnsClassDestination() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.TEACHER, new ContextClass(classId, ClassRole.TEACHER)));

        perform(publishRequest(classDestination(classId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(AnnouncementStatus.PUBLISHED.name()));

        verify(announcementRepository).save(any());
    }

    @Test
    void shouldReturnForbiddenWhenTeacherClassIsOutOfScope() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID teacherClassId = UUID.randomUUID();
        UUID otherClassId = UUID.randomUUID();

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.TEACHER, new ContextClass(teacherClassId, ClassRole.TEACHER)));

        perform(publishRequest(classDestination(otherClassId)))
                .andExpect(status().isForbidden());

        verify(announcementRepository, never()).save(any());
    }

    @Test
    void shouldPublishWhenTeacherUserDestinationIsInHisClass() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.TEACHER, new ContextClass(classId, ClassRole.TEACHER)));
        when(hubClassPort.getClassIdForUser(studentId)).thenReturn(classId);

        perform(publishRequest(userDestination(studentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(AnnouncementStatus.PUBLISHED.name()));

        verify(announcementRepository).save(any());
    }

    @Test
    void shouldReturnForbiddenWhenTeacherUserDestinationIsOutOfScope() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.TEACHER, new ContextClass(classId, ClassRole.TEACHER)));
        when(hubClassPort.getClassIdForUser(studentId)).thenReturn(UUID.randomUUID());

        perform(publishRequest(userDestination(studentId)))
                .andExpect(status().isForbidden());

        verify(announcementRepository, never()).save(any());
    }

    @Test
    void shouldReturnBadRequestWhenTitleIsBlank() throws Exception {
        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(UUID.randomUUID(), UserType.SENAI));

        PublishAnnouncementRequest request = new PublishAnnouncementRequest(
                "", "Descricao", AnnouncementOrigin.SENAI, List.of(userDestination(UUID.randomUUID())), null, null, null, null);

        perform(request).andExpect(status().isBadRequest());
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void shouldReturnBadRequestWhenThereAreNoDestinations() throws Exception {
        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(UUID.randomUUID(), UserType.SENAI));

        PublishAnnouncementRequest request = new PublishAnnouncementRequest(
                "Titulo", "Descricao", AnnouncementOrigin.SENAI, List.of(), null, null, null, null);

        perform(request).andExpect(status().isBadRequest());
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void shouldPublishWithLongDescription() throws Exception {
        UUID userId = UUID.randomUUID();
        String longDescription = "x".repeat(500);

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));

        PublishAnnouncementRequest request = new PublishAnnouncementRequest(
                "Comunicado de teste",
                longDescription,
                AnnouncementOrigin.SENAI,
                List.of(userDestination(UUID.randomUUID())),
                null,
                null,
                null,
                null
        );

        perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(AnnouncementStatus.PUBLISHED.name()));

        ArgumentCaptor<Announcement> announcementCaptor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementRepository).save(announcementCaptor.capture());
        assertEquals(500, announcementCaptor.getValue().getDescription().length());
    }

    @Test
    void shouldPublishWithShiftCodes() throws Exception {
        UUID userId = UUID.randomUUID();

        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(userId, UserType.SENAI));

        PublishAnnouncementRequest request = new PublishAnnouncementRequest(
                "Comunicado de teste",
                "Descricao do comunicado",
                AnnouncementOrigin.SENAI,
                List.of(userDestination(UUID.randomUUID())),
                null,
                null,
                List.of(ShiftCode.FULL_AM_PM),
                null
        );

        perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(AnnouncementStatus.PUBLISHED.name()));

        verify(linkTagsByCodeUseCase).execute(any(), eq(TagEntityType.SHIFT), any());
    }

    @Test
    void shouldReturnBadRequestWhenTitleExceedsMaxLength() throws Exception {
        when(requestContextProvider.getRequestContext())
                .thenReturn(createContext(UUID.randomUUID(), UserType.SENAI));

        PublishAnnouncementRequest request = new PublishAnnouncementRequest(
                "t".repeat(256),
                "Descricao valida",
                AnnouncementOrigin.SENAI,
                List.of(userDestination(UUID.randomUUID())),
                null,
                null,
                null,
                null
        );

        perform(request).andExpect(status().isBadRequest());
        verify(announcementRepository, never()).save(any());
    }

    private RequestContext createContext(UUID userId, UserType userType, ContextClass... classes) {
        return new RequestContext(userId, userType, List.of(classes));
    }

    private CreateAnnouncementDestinationInput userDestination(UUID referenceId) {
        return new CreateAnnouncementDestinationInput(AnnouncementDestinationType.USER, referenceId);
    }

    private CreateAnnouncementDestinationInput classDestination(UUID referenceId) {
        return new CreateAnnouncementDestinationInput(AnnouncementDestinationType.CLASS, referenceId);
    }

    private PublishAnnouncementRequest publishRequest(CreateAnnouncementDestinationInput... destinations) {
        return new PublishAnnouncementRequest(
                "Comunicado de teste",
                "Descricao do comunicado",
                AnnouncementOrigin.SENAI,
                List.of(destinations),
                null,
                null,
                null,
                null
        );
    }

    private org.springframework.test.web.servlet.ResultActions perform(Object body) throws Exception {
        return mockMvc.perform(post("/api/posts/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(body)));
    }
}

@WebMvcTest(AnnouncementController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublishAnnouncementWebMvcTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublishAnnouncementUseCase publishAnnouncementUseCase;

    @MockitoBean
    private ScheduleAnnouncementUseCase scheduleAnnouncementUseCase;

    @MockitoBean
    private ListAnnouncementsUseCase listAnnouncementsUseCase;

    @MockitoBean
    private ListPinnedAnnouncementsUseCase listPinnedAnnouncementsUseCase;

    @MockitoBean
    private GetAnnouncementByIdUseCase getAnnouncementByIdUseCase;

    @MockitoBean
    private ListAnnouncementFilesUseCase listAnnouncementFilesUseCase;

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

    @MockitoBean
    private ListAnnouncementHistoryUseCase listAnnouncementHistoryUseCase;

    @MockitoBean
    private PinAnnouncementUseCase pinAnnouncementUseCase;

    @MockitoBean
    private UnpinAnnouncementUseCase unpinAnnouncementUseCase;

    @MockitoBean
    private RescheduleAnnouncementUseCase rescheduleAnnouncementUseCase;

    @Test
    void shouldPublishAnnouncementByEndpoint() throws Exception {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(requestContextProvider.getRequestContext())
                .thenReturn(new RequestContext(userId, UserType.SENAI, List.of()));
        when(publishAnnouncementUseCase.execute(any()))
                .thenReturn(createPublishedAnnouncement(announcementId, userId));

        mockMvc.perform(post("/api/posts/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(announcementId.toString()))
                .andExpect(jsonPath("$.status").value(AnnouncementStatus.PUBLISHED.name()))
                .andExpect(jsonPath("$.publishedByUserId").value(userId.toString()));
    }

    @Test
    void shouldReturnForbiddenByEndpoint() throws Exception {
        UUID userId = UUID.randomUUID();

        when(requestContextProvider.getRequestContext())
                .thenReturn(new RequestContext(userId, UserType.TEACHER, List.of()));
        when(publishAnnouncementUseCase.execute(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário sem permissão!"));

        mockMvc.perform(post("/api/posts/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    private PublishAnnouncementRequest validRequest() {
        return new PublishAnnouncementRequest(
                "Comunicado publicado",
                "Descricao",
                AnnouncementOrigin.SENAI,
                List.of(new CreateAnnouncementDestinationInput(AnnouncementDestinationType.USER, UUID.randomUUID())),
                null,
                null,
                null,
                null
        );
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
