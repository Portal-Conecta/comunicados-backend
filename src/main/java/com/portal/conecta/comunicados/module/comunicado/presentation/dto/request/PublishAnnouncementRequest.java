package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import java.util.List;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.ShiftCode;
import com.portal.conecta.comunicados.shared.context.UserType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body de {@code POST /api/posts/publish} (#107): cria e publica o comunicado numa única
 * transação. O autor/publicador vem do contexto autenticado, não do body.
 */
public record PublishAnnouncementRequest(

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Título do comunicado.", example = "Reunião de alinhamento", maxLength = 255)
    String title,

    @NotBlank
    @Schema(description = "Conteúdo do comunicado em HTML sanitizado (allowlist TipTap: "
            + "p, br, strong/b, em/i, u, ul/ol/li, a[href]). O servidor strip tags perigosas "
            + "e deriva descriptionPlain automaticamente.")
    String description,

    @NotNull
    AnnouncementOrigin origin,

    @NotEmpty
    @Valid
    List<CreateAnnouncementDestinationInput> destinations,

    Boolean pinned,

    @Schema(description = "UUIDs internos da tabela tag (tag.id), não hub_entity_id do Core. "
            + "Opcional. Não use para ROLE/SHIFT — envie roles/shiftCodes.")
    List<UUID> tagIds,

    @Schema(
            description = "Turnos do comunicado (enum Shift do Core). Opcional; vazio = sem restrição de turno.",
            example = "[\"FULL_AM_PM\"]"
    )
    List<ShiftCode> shiftCodes,

    @Schema(
            description = "Papéis de usuário que podem ver o comunicado (vira tag ROLE). "
                    + "Opcional; vazio = sem restrição de papel. "
                    + "Ex.: [\"STUDENT\"] com destination GENERAL = alunos e representantes "
                    + "(representante também é tratado como aluno na visibilidade/notificação).",
            example = "[\"STUDENT\"]"
    )
    List<UserType> roles

) {

    public boolean isPinned() {
        return Boolean.TRUE.equals(pinned);
    }

    public List<ShiftCode> resolvedShiftCodes() {
        return shiftCodes == null ? List.of() : shiftCodes;
    }

    public List<UserType> resolvedRoles() {
        return roles == null ? List.of() : roles;
    }
}
