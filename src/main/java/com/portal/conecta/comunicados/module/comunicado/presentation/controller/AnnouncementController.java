package com.portal.conecta.comunicados.module.comunicado.presentation.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portal.conecta.comunicados.module.comunicado.application.command.PinAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.application.command.PublishAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.application.command.RescheduleAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.application.command.ScheduleAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.application.command.UnpinAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.application.command.UpdateAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.application.query.GetAnnouncementByIdQuery;
import com.portal.conecta.comunicados.module.comunicado.application.query.ListAnnouncementHistoryQuery;
import com.portal.conecta.comunicados.module.comunicado.application.query.ListAnnouncementsQuery;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.DeleteAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.GetAnnouncementByIdUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.ListAnnouncementHistoryUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.ListAnnouncementsUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.ListPinnedAnnouncementsUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.PinAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.PublishAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.RescheduleAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.ScheduleAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.UnpinAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.UpdateAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.AnnouncementHistoryFilterRequest;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.PinAnnouncementRequest;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.PostFilterRequest;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.PublishAnnouncementRequest;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.RescheduleAnnouncementRequest;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.ScheduleAnnouncementRequest;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.UpdateAnnouncementRequest;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.response.AnnouncementDetailResponse;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.response.AnnouncementResponse;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.response.ListAnnouncementHistoryResponse;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.response.ListAnnouncementsResponse;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.response.ListPinnedAnnouncementsResponse;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts")
@Tag(name = "Postagens", description = "Endpoints de comunicados")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class AnnouncementController {

    private final PublishAnnouncementUseCase publishAnnouncementUseCase;
    private final ScheduleAnnouncementUseCase scheduleAnnouncementUseCase;
    private final ListAnnouncementsUseCase listAnnouncementsUseCase;
    private final ListPinnedAnnouncementsUseCase listPinnedAnnouncementsUseCase;
    private final GetAnnouncementByIdUseCase getAnnouncementByIdUseCase;
    private final DeleteAnnouncementUseCase deleteAnnouncementUseCase;
    private final RequestContextProvider contextProvider;
    private final PinAnnouncementUseCase pinAnnouncementUseCase;
    private final UnpinAnnouncementUseCase unpinAnnouncementUseCase;
    private final UpdateAnnouncementUseCase updateAnnouncementUseCase;
    private final ListAnnouncementHistoryUseCase listAnnouncementHistoryUseCase;
    private final RescheduleAnnouncementUseCase rescheduleAnnouncementUseCase;


    @Operation(
            summary = "Listar comunicados",
            description = "Lista paginada de comunicados visíveis ao perfil autenticado, em ordem "
                    + "cronológica decrescente. Comunicados removidos nunca aparecem. Aceita filtros "
                    + "por origem (WEG/SENAI/BOTH), turma, intervalo de publicação e termo de busca "
                    + "textual (`search`) em título, descrição, tags e destinatários."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de filtro inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping
    public ResponseEntity<ListAnnouncementsResponse> list(@Valid @ModelAttribute PostFilterRequest filter) {
        UUID userId = contextProvider.getRequestContext().userId();

        ListAnnouncementsQuery query = new ListAnnouncementsQuery(filter, userId);
        Page<Announcement> page = listAnnouncementsUseCase.execute(query);

        return ResponseEntity.ok(ListAnnouncementsResponse.fromPage(page));
    }

    @Operation(summary = "Buscar comunicado por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comunicado encontrado"),
            @ApiResponse(responseCode = "404", description = "Não encontrado ou fora do escopo"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementDetailResponse> getById(@PathVariable UUID id) {
        UUID userId = contextProvider.getRequestContext().userId();

        GetAnnouncementByIdQuery query = new GetAnnouncementByIdQuery(id, userId);
        Announcement announcement = getAnnouncementByIdUseCase.execute(query);

        return ResponseEntity.ok(AnnouncementDetailResponse.fromEntity(announcement));
    }

    @Operation(
            summary = "Consultar histórico do comunicado",
            description = "Lista o histórico de auditoria do comunicado, respeitando a mesma regra de visibilidade da consulta por ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico retornado"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Não encontrado ou fora do escopo")
    })
    @GetMapping("/{id}/history")
    public ResponseEntity<ListAnnouncementHistoryResponse> history(
            @PathVariable UUID id,
            @Valid @ModelAttribute AnnouncementHistoryFilterRequest filter
    ) {
        UUID userId = contextProvider.getRequestContext().userId();

        ListAnnouncementHistoryQuery query = new ListAnnouncementHistoryQuery(id, userId, filter);
        Page<AnnouncementHistory> page = listAnnouncementHistoryUseCase.execute(query);

        return ResponseEntity.ok(ListAnnouncementHistoryResponse.fromPage(page));
    }

    @Operation(summary = "Atualizar comunicado completo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atualizado"),
            @ApiResponse(responseCode = "403", description = "Sem permissao"),
            @ApiResponse(responseCode = "404", description = "Nao encontrado"),
            @ApiResponse(responseCode = "409", description = "Comunicado removido nao pode ser editado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<AnnouncementResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAnnouncementRequest request
    ) {
        UUID userId = contextProvider.getRequestContext().userId();
        UpdateAnnouncementCommand command = UpdateAnnouncementCommand.fromRequest(id, request, userId);

        Announcement updated = updateAnnouncementUseCase.execute(command);

        return ResponseEntity.ok(AnnouncementResponse.fromEntity(updated));
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
        deleteAnnouncementUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Publicar comunicado",
            description = "Cria e publica o comunicado numa única transação (#107): nasce PUBLISHED, com "
                    + "destinos e histórico (CREATION + PUBLICATION). O autor/publicador é o usuário autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comunicado criado e publicado"),
            @ApiResponse(responseCode = "400", description = "Dados obrigatórios ausentes ou inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão / fora do escopo (PA02/PA03)")
    })
    @PostMapping("/publish")
    public ResponseEntity<AnnouncementResponse> publish(@Valid @RequestBody PublishAnnouncementRequest request) {
        RequestContext context = contextProvider.getRequestContext();

        PublishAnnouncementCommand command = PublishAnnouncementCommand.from(request, context);
        Announcement published = publishAnnouncementUseCase.execute(command);

        return created(published);
    }

    @Operation(
            summary = "Agendar comunicado",
            description = "Cria e agenda o comunicado numa única transação (#108): nasce SCHEDULED, com "
                    + "destinos e histórico (CREATION + SCHEDULED). scheduledFor precisa ser futuro."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comunicado criado e agendado"),
            @ApiResponse(responseCode = "400", description = "Data no passado ou dados obrigatórios ausentes"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão / fora do escopo")
    })
    @PostMapping("/schedule")
    public ResponseEntity<AnnouncementResponse> schedule(@Valid @RequestBody ScheduleAnnouncementRequest request) {
        RequestContext context = contextProvider.getRequestContext();

        ScheduleAnnouncementCommand command = ScheduleAnnouncementCommand.from(request, context);
        Announcement scheduled = scheduleAnnouncementUseCase.execute(command);

        return created(scheduled);
    }

    private ResponseEntity<AnnouncementResponse> created(Announcement announcement) {
        return ResponseEntity
                .created(URI.create("/api/posts/" + announcement.getId()))
                .body(AnnouncementResponse.fromEntity(announcement));
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
    public ResponseEntity<AnnouncementResponse> pin(@PathVariable UUID id, @Valid @RequestBody PinAnnouncementRequest request) {
        UUID pinnedByUserId = contextProvider.getRequestContext().userId();

        PinAnnouncementCommand pinAnnouncementCommand = PinAnnouncementCommand.from(request, pinnedByUserId, id);

        Announcement announcement = pinAnnouncementUseCase.execute(pinAnnouncementCommand);

        return ResponseEntity.ok(AnnouncementResponse.fromEntity(announcement));
    }

    @Operation(summary = "Desafixar comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Desafixado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @PatchMapping("/{id}/unpin")
    public ResponseEntity<AnnouncementResponse> unpin(@PathVariable UUID id) {
        UUID unpinnedByUserId = contextProvider.getRequestContext().userId();

        UnpinAnnouncementCommand unpinAnnouncementCommand = UnpinAnnouncementCommand.from(id, unpinnedByUserId);

        Announcement announcement = unpinAnnouncementUseCase.execute(unpinAnnouncementCommand);

        return ResponseEntity.ok(AnnouncementResponse.fromEntity(announcement));
    }

    @Operation(
            summary = "Listar comunicados fixados",
            description = "Retorna comunicados publicados e fixados visíveis ao perfil autenticado, "
                    + "ordenados por pinnedOrder crescente. Lista vazia quando não houver fixados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping("/pinned")
    public ResponseEntity<ListPinnedAnnouncementsResponse> listPinned() {
        List<Announcement> announcements = listPinnedAnnouncementsUseCase.execute();

        return ResponseEntity.ok(ListPinnedAnnouncementsResponse.fromEntities(announcements));
    }

    @Operation(summary = "Reagendar comunicado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comunicado reagendado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Data ausente, inválida ou no passado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "404", description = "Comunicado inexistente ou removido"),
            @ApiResponse(responseCode = "409", description = "Comunicado não está em status SCHEDULED")
    })
    @PatchMapping("/{id}/schedule")
    public ResponseEntity<AnnouncementResponse> reschedule(
            @PathVariable UUID id,
            @Valid @RequestBody RescheduleAnnouncementRequest request
    ) {
        UUID userId = contextProvider.getRequestContext().userId();
        RescheduleAnnouncementCommand command = RescheduleAnnouncementCommand.from(request, id, userId);

        Announcement rescheduled = rescheduleAnnouncementUseCase.execute(command);

        return ResponseEntity.ok(AnnouncementResponse.fromEntity(rescheduled));
    }
}
