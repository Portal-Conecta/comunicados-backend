package com.portal.conecta.comunicados;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.portal.conecta.comunicados.module.comunicado.application.dto.AnnouncementFileView;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.AttachAnnouncementFileUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.ListAnnouncementFilesUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.PresignUploadAnnouncementFileUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.RemoveAnnouncementFileUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.SetAnnouncementThumbnailUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileType;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementFileNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementFileLimitExceededException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementFileTooLargeException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementPermissionDeniedException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;
import com.portal.conecta.comunicados.module.comunicado.presentation.controller.PostImageController;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import com.portal.conecta.comunicados.shared.context.UserType;
import com.portal.conecta.comunicados.shared.security.config.SecurityConfig;
import com.portal.conecta.comunicados.shared.security.filter.JwtAuthenticationFilter;

@WebMvcTest(
        controllers = PostImageController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class PostImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private AttachAnnouncementFileUseCase attachFileUseCase;
    @MockitoBean private PresignUploadAnnouncementFileUseCase presignUploadUseCase;
    @MockitoBean private RemoveAnnouncementFileUseCase removeFileUseCase;
    @MockitoBean private SetAnnouncementThumbnailUseCase setThumbnailUseCase;
    @MockitoBean private ListAnnouncementFilesUseCase listFilesUseCase;
    @MockitoBean private RequestContextProvider contextProvider;

    private static final UUID POST_ID = UUID.randomUUID();
    private static final UUID FILE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private void stubContext() {
        when(contextProvider.getRequestContext())
                .thenReturn(new RequestContext(USER_ID, UserType.SENAI, List.of()));
    }

    private AnnouncementFile stubFile() {
        return AnnouncementFile.builder()
                .id(FILE_ID)
                .originalName("foto.jpg")
                .s3Key("key-abc")
                .s3Bucket("bucket")
                .contentType("image/jpeg")
                .type(AnnouncementFileType.IMAGE)
                .sizeBytes(1024L)
                .isThumbnail(false)
                .uploadedByUserId(USER_ID)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void shouldReturn201WhenUploadSucceeds() throws Exception {
        stubContext();
        when(attachFileUseCase.execute(any())).thenReturn(stubFile());

        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/posts/{postId}/images", POST_ID).file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalName").value("foto.jpg"));
    }

    @Test
    void shouldReturn403WhenUploadIsForbidden() throws Exception {
        stubContext();
        when(attachFileUseCase.execute(any())).thenThrow(new AnnouncementPermissionDeniedException());

        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/api/posts/{postId}/images", POST_ID).file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn400WhenFileLimitIsExceeded() throws Exception {
        stubContext();
        when(attachFileUseCase.execute(any())).thenThrow(new AnnouncementFileLimitExceededException());

        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/api/posts/{postId}/images", POST_ID).file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn413WhenFileIsTooLarge() throws Exception {
        stubContext();
        when(attachFileUseCase.execute(any())).thenThrow(new AnnouncementFileTooLargeException());

        MockMultipartFile file = new MockMultipartFile("file", "grande.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/api/posts/{postId}/images", POST_ID).file(file))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void shouldReturn200WhenListingFiles() throws Exception {
        stubContext();
        AnnouncementFile file = stubFile();
        file.setFileStatus(AnnouncementFileStatus.READY);
        when(listFilesUseCase.execute(POST_ID))
                .thenReturn(List.of(new AnnouncementFileView(file, "https://s3.example.com/signed")));

        mockMvc.perform(get("/api/posts/{postId}/images", POST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].originalName").value("foto.jpg"))
                .andExpect(jsonPath("$[0].fileStatus").value("READY"))
                .andExpect(jsonPath("$[0].displayUrl").value("https://s3.example.com/signed"));
    }

    @Test
    void shouldReturnNullDisplayUrlForPendingFile() throws Exception {
        stubContext();
        AnnouncementFile file = stubFile();
        file.setFileStatus(AnnouncementFileStatus.PENDING);
        when(listFilesUseCase.execute(POST_ID))
                .thenReturn(List.of(new AnnouncementFileView(file, null)));

        mockMvc.perform(get("/api/posts/{postId}/images", POST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileStatus").value("PENDING"))
                .andExpect(jsonPath("$[0].displayUrl").doesNotExist());
    }

    @Test
    void shouldReturn204WhenDeleteSucceeds() throws Exception {
        stubContext();

        mockMvc.perform(delete("/api/posts/{postId}/images/{imageId}", POST_ID, FILE_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonexistentFile() throws Exception {
        stubContext();
        doThrow(new AnnouncementFileNotFoundException()).when(removeFileUseCase).execute(any());

        mockMvc.perform(delete("/api/posts/{postId}/images/{imageId}", POST_ID, FILE_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn200WhenSettingThumbnail() throws Exception {
        stubContext();
        AnnouncementFile withThumbnail = AnnouncementFile.builder()
                .id(FILE_ID)
                .originalName("capa.jpg")
                .s3Key("key-abc")
                .s3Bucket("bucket")
                .contentType("image/jpeg")
                .type(AnnouncementFileType.IMAGE)
                .sizeBytes(1024L)
                .isThumbnail(true)
                .uploadedByUserId(USER_ID)
                .createdAt(Instant.now())
                .build();

        when(setThumbnailUseCase.execute(any())).thenReturn(withThumbnail);

        mockMvc.perform(patch("/api/posts/{postId}/images/{imageId}/thumbnail", POST_ID, FILE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isThumbnail").value(true));
    }
}
