package com.portal.conecta.comunicados;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.portal.conecta.comunicados.module.tag.application.usecase.LinkAnnouncementTagUseCase;
import com.portal.conecta.comunicados.module.tag.application.usecase.ListAnnouncementTagsUseCase;
import com.portal.conecta.comunicados.module.tag.application.usecase.UnlinkAnnouncementTagUseCase;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.UserType;

/**
 * Teste de integração (#130): com o contexto completo (ambos os controllers carregados), as rotas
 * {@code /api/posts/{id}/tags} devem resolver para o {@code TagController} — não para os stubs
 * removidos do {@code AnnouncementController}. {@code @WebMvcTest} carrega um controller por vez e
 * não detectaria a colisão; este {@code @SpringBootTest} sim.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnnouncementTagRoutesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListAnnouncementTagsUseCase listAnnouncementTagsUseCase;

    @MockitoBean
    private LinkAnnouncementTagUseCase linkAnnouncementTagUseCase;

    @MockitoBean
    private UnlinkAnnouncementTagUseCase unlinkAnnouncementTagUseCase;

    private Authentication auth() {
        RequestContext principal = new RequestContext(UUID.randomUUID(), UserType.SENAI, List.of());
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    @Test
    void getPostTagsShouldResolveToTagController() throws Exception {
        when(listAnnouncementTagsUseCase.execute(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/posts/{id}/tags", UUID.randomUUID()).with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(listAnnouncementTagsUseCase).execute(any());
    }

    @Test
    void postPostTagsShouldResolveToTagController() throws Exception {
        when(linkAnnouncementTagUseCase.execute(any())).thenReturn(List.of());

        String body = "{\"tagIds\":[\"" + UUID.randomUUID() + "\"]}";

        mockMvc.perform(post("/api/posts/{id}/tags", UUID.randomUUID())
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(linkAnnouncementTagUseCase).execute(any());
    }

    @Test
    void deletePostTagShouldResolveToTagController() throws Exception {
        mockMvc.perform(delete("/api/posts/{id}/tags/{tagId}", UUID.randomUUID(), UUID.randomUUID())
                        .with(authentication(auth())))
                .andExpect(status().isNoContent());

        verify(unlinkAnnouncementTagUseCase).execute(any());
    }
}
