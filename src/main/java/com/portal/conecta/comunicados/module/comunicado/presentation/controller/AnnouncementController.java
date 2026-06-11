package com.portal.conecta.comunicados.module.comunicado.presentation.controller;

import com.portal.conecta.comunicados.module.comunicado.application.command.PublishAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.PublishAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.response.AnnouncementResponse;
import com.portal.conecta.comunicados.module.comunicado.presentation.mapper.AnnouncementMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@Tag(name = "Postagens", description = "Endpoints de comunicados")
public class AnnouncementController {

    private final PublishAnnouncementUseCase publishAnnouncementUseCase;
    private final AnnouncementMapper announcementMapper;

    public AnnouncementController(
            PublishAnnouncementUseCase publishAnnouncementUseCase,
            AnnouncementMapper announcementMapper
    ) {
        this.publishAnnouncementUseCase = publishAnnouncementUseCase;
        this.announcementMapper = announcementMapper;
    }

    @Operation(summary = "Criar comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Validação falhou"),
            @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @PostMapping
    public ResponseEntity<Object> save(@Valid @RequestBody Object request) {
        return ResponseEntity.created(URI.create("/api/posts/")).body(null);
    }

    @Operation(summary = "Listar comunicados")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada"),
            @ApiResponse(responseCode = "403", description = "Escopo negado")
    })
    @GetMapping
    public ResponseEntity<Object> list() {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "Buscar comunicado por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comunicado encontrado"),
            @ApiResponse(responseCode = "404", description = "Não encontrado"),
            @ApiResponse(responseCode = "403", description = "Fora do escopo")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Object> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "Atualizar comunicado completo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atualizado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "404", description = "Não encontrado"),
            @ApiResponse(responseCode = "422", description = "Regra de negócio violada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable UUID id, @Valid @RequestBody Object request) {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "Atualizar comunicado parcialmente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atualizado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "404", description = "Não encontrado")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<Object> partialUpdate(@PathVariable UUID id, @Valid @RequestBody Object request) {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "Remover comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Removido"),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "404", description = "Não encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Publicar comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Publicado"),
            @ApiResponse(responseCode = "400", description = "Dados obrigatórios ausentes"),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "404", description = "Comunicado não encontrado"),
            @ApiResponse(responseCode = "409", description = "Comunicado não pode ser publicado nesse status")
    })
    @PatchMapping("/{id}/publish")
    public ResponseEntity<AnnouncementResponse> publish(@PathVariable UUID id) {
        var command = PublishAnnouncementCommand.from(id);
        var announcement = publishAnnouncementUseCase.execute(command);
        var response = announcementMapper.toResponse(announcement);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Agendar comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão — APRENDIZ"),
            @ApiResponse(responseCode = "422", description = "AG02 — data no passado")
    })
    @PatchMapping("/{id}/schedule")
    public ResponseEntity<Object> schedule(@PathVariable UUID id, @Valid @RequestBody Object request) {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "Cancelar agendamento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendamento cancelado"),
            @ApiResponse(responseCode = "404", description = "Não encontrado")
    })
    @PatchMapping("/{id}/cancel-schedule")
    public ResponseEntity<Object> cancelSchedule(@PathVariable UUID id) {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "Fixar comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fixado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão — DOCENTE/APRENDIZ")
    })
    @PatchMapping("/{id}/pin")
    public ResponseEntity<Object> pin(@PathVariable UUID id, @RequestBody(required = false) Object request) {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "Desafixar comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Desafixado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @PatchMapping("/{id}/unpin")
    public ResponseEntity<Object> unpin(@PathVariable UUID id) {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "Listar comunicados fixados")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada")
    })
    @GetMapping("/pinned")
    public ResponseEntity<List<Object>> listPinned() {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "Listar tags do comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tags retornadas"),
            @ApiResponse(responseCode = "404", description = "Comunicado não encontrado")
    })
    @GetMapping("/{postId}/tags")
    public ResponseEntity<Object> listAnnouncementTag(@PathVariable UUID postId) {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "Vincular tag ao comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tag vinculada"),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "422", description = "TAG03 — tag inativa")
    })
    @PostMapping("/{postId}/tags")
    public ResponseEntity<Object> linkAnnouncementTag(@PathVariable UUID postId, @Valid @RequestBody Object request) {
        return ResponseEntity.created(URI.create("/api/posts/" + postId + "/tags")).body(null);
    }

    @Operation(summary = "Desvincular tag do comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tag desvinculada"),
            @ApiResponse(responseCode = "404", description = "Não encontrado")
    })
    @DeleteMapping("/{postId}/tags/{tagId}")
    public ResponseEntity<Void> unlinkAnnouncementTag(@PathVariable UUID postId, @PathVariable UUID tagId) {
        return ResponseEntity.noContent().build();
    }

}