package com.portal.conecta.comunicados.module.comunicado.presentation.dto.request;

import com.portal.conecta.comunicados.module.comunicado.domain.model.IndividualNoticeCategory;

import jakarta.validation.constraints.NotBlank;

public record CreateIndividualNoticeCategoryRequest(

    @NotBlank
    String name

) {

    public static CreateIndividualNoticeCategoryRequest fromEntity(IndividualNoticeCategory entity) {
        return new CreateIndividualNoticeCategoryRequest(entity.getName());
    }
}