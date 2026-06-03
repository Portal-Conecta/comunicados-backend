package com.portal.conecta.comunicados.module.comunicado.presentation.mapper;

import com.portal.conecta.comunicados.module.comunicado.application.command.CreateIndividualNoticeCategoryCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.model.IndividualNoticeCategory;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateIndividualNoticeCategoryRequest;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.response.IndividualNoticeCategoryResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface IndividualNoticeCategoryMapper {

    IndividualNoticeCategoryResponse toResponse(IndividualNoticeCategory entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    IndividualNoticeCategory toEntity(CreateIndividualNoticeCategoryRequest request);

    @BeanMapping(
            ignoreByDefault = true,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "name", source = "name")
    void applyUpdate(CreateIndividualNoticeCategoryRequest request, @MappingTarget IndividualNoticeCategory entity);

    default CreateIndividualNoticeCategoryCommand toCreateCommand(
            CreateIndividualNoticeCategoryRequest request,
            UUID actorUserId
    ) {
        return new CreateIndividualNoticeCategoryCommand(request, actorUserId);
    }
    
}