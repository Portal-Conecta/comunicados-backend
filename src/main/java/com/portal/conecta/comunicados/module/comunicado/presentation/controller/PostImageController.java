package com.portal.conecta.comunicados.module.comunicado.presentation.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.portal.conecta.comunicados.module.comunicado.application.command.AttachAnnouncementFileCommand;
import com.portal.conecta.comunicados.module.comunicado.application.command.PresignUploadAnnouncementFileCommand;
import com.portal.conecta.comunicados.module.comunicado.application.command.RemoveAnnouncementFileCommand;
import com.portal.conecta.comunicados.module.comunicado.application.command.SetAnnouncementThumbnailCommand;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.AttachAnnouncementFileUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.ListAnnouncementFilesUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.PresignUploadAnnouncementFileUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.RemoveAnnouncementFileUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.SetAnnouncementThumbnailUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.dto.AnnouncementFileView;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.PresignUploadRequest;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.response.AnnouncementFileResponse;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.response.PresignUploadResponse;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts/{postId}/images")
@Tag(name = "Imagens", description = "Endpoints de imagens do comunicado")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class PostImageController {

    private final AttachAnnouncementFileUseCase attachFileUseCase;
    private final PresignUploadAnnouncementFileUseCase presignUploadUseCase;
    private final RemoveAnnouncementFileUseCase removeFileUseCase;
    private final SetAnnouncementThumbnailUseCase setThumbnailUseCase;
    private final ListAnnouncementFilesUseCase listFilesUseCase;
    private final RequestContextProvider contextProvider;

    @Operation(summary = "Solicitar URL pré-assinada para upload direto ao S3")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "URL gerada — faça PUT direto ao S3"),
            @ApiResponse(responseCode = "400", description = "ARQ02 — limite de 5 arquivos atingido, tipo inválido ou tamanho excedido"),
            @ApiResponse(responseCode = "403", description = "Sem permissão de edição"),
            @ApiResponse(responseCode = "404", description = "Comunicado não encontrado")
    })
    @PostMapping("/presign")
    public ResponseEntity<PresignUploadResponse> presign(
            @PathVariable UUID postId,
            @Valid @RequestBody PresignUploadRequest request
    ) {
        UUID userId = contextProvider.getRequestContext().userId();
        PresignUploadAnnouncementFileCommand command = new PresignUploadAnnouncementFileCommand(
                postId,
                request.contentType(),
                request.sizeBytes(),
                request.originalName(),
                request.thumbnail(),
                userId
        );
        PresignUploadResponse response = presignUploadUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar arquivos do comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada"),
            @ApiResponse(responseCode = "404", description = "Comunicado não encontrado")
    })
    @GetMapping
    public ResponseEntity<List<AnnouncementFileResponse>> list(@PathVariable UUID postId) {
        List<AnnouncementFileView> views = listFilesUseCase.execute(postId);
        return ResponseEntity.ok(AnnouncementFileResponse.fromViews(views));
    }

    @Operation(summary = "Anexar arquivo ao comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Arquivo anexado"),
            @ApiResponse(responseCode = "400", description = "ARQ02 — limite de 5 arquivos atingido ou tipo inválido"),
            @ApiResponse(responseCode = "403", description = "Sem permissão de edição"),
            @ApiResponse(responseCode = "404", description = "Comunicado não encontrado"),
            @ApiResponse(responseCode = "413", description = "ARQ06 — arquivo maior que 10MB")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnnouncementFileResponse> upload(
            @PathVariable UUID postId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "false") boolean thumbnail
    ) {
        UUID userId = contextProvider.getRequestContext().userId();
        AttachAnnouncementFileCommand command = AttachAnnouncementFileCommand.from(postId, file, thumbnail, userId);
        AnnouncementFile saved = attachFileUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(AnnouncementFileResponse.fromEntity(saved));
    }

    @Operation(summary = "Remover arquivo do comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Arquivo removido"),
            @ApiResponse(responseCode = "403", description = "Sem permissão de edição"),
            @ApiResponse(responseCode = "404", description = "Arquivo não encontrado")
    })
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID postId,
            @PathVariable UUID imageId
    ) {
        UUID userId = contextProvider.getRequestContext().userId();
        removeFileUseCase.execute(RemoveAnnouncementFileCommand.of(imageId, userId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Definir miniatura do comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Miniatura definida"),
            @ApiResponse(responseCode = "403", description = "Sem permissão de edição"),
            @ApiResponse(responseCode = "404", description = "Arquivo não encontrado no comunicado")
    })
    @PatchMapping("/{imageId}/thumbnail")
    public ResponseEntity<AnnouncementFileResponse> setThumbnail(
            @PathVariable UUID postId,
            @PathVariable UUID imageId
    ) {
        UUID userId = contextProvider.getRequestContext().userId();
        SetAnnouncementThumbnailCommand command = SetAnnouncementThumbnailCommand.of(postId, imageId, userId);
        AnnouncementFile updated = setThumbnailUseCase.execute(command);
        return ResponseEntity.ok(AnnouncementFileResponse.fromEntity(updated));
    }
}
