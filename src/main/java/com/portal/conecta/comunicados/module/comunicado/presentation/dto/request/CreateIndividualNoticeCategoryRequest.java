package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateIndividualNoticeCategoryRequest(

    @NotBlank
    String name

) {}