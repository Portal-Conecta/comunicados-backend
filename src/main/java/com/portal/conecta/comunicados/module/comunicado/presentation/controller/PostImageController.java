package com.portal.conecta.comunicados.module.comunicado.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/posts/{postId}/images")
@Tag(name = "Imagens", description = "Endpoints de imagens do comunicado")
public class PostImageController {

    @Operation(summary = "Anexar imagem ao comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Imagem anexada"),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "413", description = "ARQ06 — arquivo maior que 10MB"),
            @ApiResponse(responseCode = "422", description = "ARQ02 — mais de 5 arquivos")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> upload(
            @PathVariable UUID postId,
            @RequestPart("file") MultipartFile file,
            @RequestPart Object meta) {
        return ResponseEntity.status(201).body(null);
    }

    @Operation(summary = "Listar imagens do comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada"),
            @ApiResponse(responseCode = "404", description = "Comunicado não encontrado")
    })
    @GetMapping
    public ResponseEntity<Object> list(@PathVariable UUID postId) {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "Remover imagem do comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Removida"),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "404", description = "Não encontrada")
    })
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID postId,
            @PathVariable UUID imageId) {
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Definir miniatura do comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Miniatura definida"),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "422", description = "IM04 — já existe miniatura")
    })
    @PatchMapping("/{imageId}/thumbnail")
    public ResponseEntity<Object> setThumbnail(
            @PathVariable UUID postId,
            @PathVariable UUID imageId) {
        return ResponseEntity.ok(null);
    }
}