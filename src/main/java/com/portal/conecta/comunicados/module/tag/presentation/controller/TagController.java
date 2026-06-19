package com.portal.conecta.comunicados.module.tag.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementTag;
import com.portal.conecta.comunicados.module.tag.application.command.LinkAnnouncementTagCommand;
import com.portal.conecta.comunicados.module.tag.application.command.UnlinkAnnouncementTagCommand;
import com.portal.conecta.comunicados.module.tag.application.usecase.LinkAnnouncementTagUseCase;
import com.portal.conecta.comunicados.module.tag.application.usecase.ListAnnouncementTagsUseCase;
import com.portal.conecta.comunicados.module.tag.application.usecase.ListTagsUseCase;
import com.portal.conecta.comunicados.module.tag.application.usecase.UnlinkAnnouncementTagUseCase;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.presentation.dto.request.LinkAnnouncementTagRequest;
import com.portal.conecta.comunicados.module.tag.presentation.dto.response.TagResponse;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Tags", description = "Endpoints de tags")
@SecurityRequirement(name = "BearerAuth")
public class TagController {

    private final ListTagsUseCase listTagsUseCase;
    private final ListAnnouncementTagsUseCase listAnnouncementTagsUseCase;
    private final LinkAnnouncementTagUseCase linkAnnouncementTagUseCase;
    private final UnlinkAnnouncementTagUseCase unlinkAnnouncementTagUseCase;
    private final RequestContextProvider contextProvider;

    @Operation(summary = "Listar tags ativas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada"),
            @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @GetMapping("/api/tags")
    public ResponseEntity<List<TagResponse>> listTags(
            @RequestParam(required = false) TagEntityType entityType
    ) {
        List<TagResponse> response = listTagsUseCase.execute(entityType)
                .stream()
                .map(TagResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar tags do comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tags retornadas"),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "404", description = "Comunicado não encontrado")
    })
    @GetMapping("/api/posts/{id}/tags")
    public ResponseEntity<List<TagResponse>> listAnnouncementTags(@PathVariable UUID id) {
        List<TagResponse> response = listAnnouncementTagsUseCase.execute(id)
                .stream()
                .map(AnnouncementTag::getTag)
                .map(TagResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Associar tags ao comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tags associadas"),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "404", description = "Comunicado ou tag não encontrada"),
            @ApiResponse(responseCode = "409", description = "Tag já associada")
    })
    @PostMapping("/api/posts/{id}/tags")
    public ResponseEntity<List<TagResponse>> linkTags(
            @PathVariable UUID id,
            @Valid @RequestBody LinkAnnouncementTagRequest request
    ) {
        UUID userId = contextProvider.getRequestContext().userId();
        LinkAnnouncementTagCommand command = LinkAnnouncementTagCommand.from(id, request, userId);

        List<TagResponse> response = linkAnnouncementTagUseCase.execute(command)
                .stream()
                .map(AnnouncementTag::getTag)
                .map(TagResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Desassociar tag do comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tag desassociada"),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "404", description = "Comunicado não encontrado")
    })
    @DeleteMapping("/api/posts/{id}/tags/{tagId}")
    public ResponseEntity<Void> unlinkTag(
            @PathVariable UUID id,
            @PathVariable UUID tagId
    ) {
        UUID userId = contextProvider.getRequestContext().userId();
        UnlinkAnnouncementTagCommand command = UnlinkAnnouncementTagCommand.from(id, tagId, userId);

        unlinkAnnouncementTagUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }
}