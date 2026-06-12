package com.portal.conecta.comunicados;

import com.portal.conecta.comunicados.module.comunicado.application.usecase.DeleteAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.GetAnnouncementByIdUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.ListAnnouncementsUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.PublishAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.ScheduleAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.presentation.controller.AnnouncementController;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import com.portal.conecta.comunicados.shared.context.UserType;
import com.portal.conecta.comunicados.shared.security.config.SecurityConfig;
import com.portal.conecta.comunicados.shared.security.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AnnouncementController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class AnnouncementControllerTest {

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
    private RequestContextProvider contextProvider;

    private void stubContext() {
        when(contextProvider.getRequestContext())
                .thenReturn(new RequestContext(UUID.randomUUID(), UserType.SENAI, List.of()));
    }

    @Test
    void shouldReturn200AndItems_WhenListing() throws Exception {
        stubContext();

        Announcement announcement = Announcement.builder()
                .id(UUID.randomUUID())
                .title("Comunicado")
                .build();

        when(listAnnouncementsUseCase.execute(any()))
                .thenReturn(new PageImpl<>(List.of(announcement)));

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].title").value("Comunicado"));
    }

    @Test
    void shouldReturn200AndDetail_WhenFound() throws Exception {
        stubContext();

        UUID id = UUID.randomUUID();
        Announcement announcement = Announcement.builder()
                .id(id)
                .title("Comunicado")
                .files(List.of())
                .tags(List.of())
                .mentions(List.of())
                .destinations(List.of())
                .build();

        when(getAnnouncementByIdUseCase.execute(any())).thenReturn(announcement);

        mockMvc.perform(get("/api/posts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.announcement.id").value(id.toString()));
    }

    @Test
    void shouldReturn405WhenCreatingAnnouncementViaPost() throws Exception {
        stubContext();

        mockMvc.perform(post("/api/posts")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Comunicado",
                                  "description": "Descrição",
                                  "origin": "SENAI",
                                  "status": "DRAFT",
                                  "destinations": []
                                }
                                """))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void shouldReturn200AndItems_WhenListingWithSearch() throws Exception {
        stubContext();

        when(listAnnouncementsUseCase.execute(any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/posts").param("search", "retirada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void shouldReturn404_WhenNotFound() throws Exception {
        stubContext();

        when(getAnnouncementByIdUseCase.execute(any()))
                .thenThrow(new AnnouncementNotFoundException());

        mockMvc.perform(get("/api/posts/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
