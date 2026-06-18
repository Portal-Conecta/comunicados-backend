package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springdoc.core.annotations.ParameterObject;

@ParameterObject
public record AnnouncementHistoryFilterRequest (

    @Schema(description = "Página da consulta", example = "0")
    @Min(0)
    Integer page,

    @Schema(description = "Quantidade de itens por página", example = "20")
    @Min(1) @Max(100)
    Integer size

) {

    private static  final int DEFAULT_PAGE = 0;
    private static  final int DEFAULT_SIZE = 20;

    public AnnouncementHistoryFilterRequest {
        if (page == null) {
            page = DEFAULT_PAGE;
        }

        if (size == null || size <= 0) {
            size = DEFAULT_SIZE;
           }
    }

}