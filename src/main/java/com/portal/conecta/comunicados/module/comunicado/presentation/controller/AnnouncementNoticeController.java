package com.portal.conecta.comunicados.module.comunicado.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Tag(name = "Avisos", description = "Endpoints de avisos individuais")
public class AnnouncementNoticeController {

    @Operation(summary = "Criar aviso individual")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Aviso criado"),
            @ApiResponse(responseCode = "403", description = "AVI03 — sem permissão"),
            @ApiResponse(responseCode = "422", description = "AVI07 — categoria duplicada ativa")
    })
    @PostMapping("/api/posts/{postId}/notices")
    public ResponseEntity<Object> create(
            @PathVariable UUID postId,
            @Valid @RequestBody Object request) {
        return ResponseEntity.status(201).body(null);
    }

    @Operation(summary = "Listar avisos do comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada"),
            @ApiResponse(responseCode = "403", description = "Fora do escopo")
    })
    @GetMapping("/api/posts/{postId}/notices")
    public ResponseEntity<Object> list(@PathVariable UUID postId) {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "Resolver aviso")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aviso resolvido"),
            @ApiResponse(responseCode = "403", description = "AVI06 — sem permissão"),
            @ApiResponse(responseCode = "422", description = "Já resolvido")
    })
    @PatchMapping("/api/notices/{noticeId}/resolve")
    public ResponseEntity<Object> resolve(
            @PathVariable UUID noticeId,
            @RequestBody(required = false) Object request) {
        return ResponseEntity.ok(null);
    }
}
