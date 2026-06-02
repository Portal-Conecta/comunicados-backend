package com.portal.conecta.comunicados.module.tag.presentation.mapper;

import com.portal.conecta.comunicados.module.tag.application.command.CreateTagCommand;
import com.portal.conecta.comunicados.module.tag.application.command.UpdateTagCommand;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.presentation.dto.request.CreateTagRequest;
import com.portal.conecta.comunicados.module.tag.presentation.dto.request.UpdateTagRequest;
import com.portal.conecta.comunicados.module.tag.presentation.dto.response.TagResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface TagMapper {

    TagResponse toResponse(Tag entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "entityType", source = "entityType")
    @Mapping(target = "active", source = "active")
    Tag toEntity(CreateTagRequest request);

    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "active", source = "active")
    void applyUpdate(UpdateTagRequest request, @MappingTarget Tag entity);

    default CreateTagCommand toCreateCommand(CreateTagRequest request) {
        return new CreateTagCommand(request);
    }

    default UpdateTagCommand toUpdateCommand(UUID id, UpdateTagRequest request) {
        return new UpdateTagCommand(id, request);
    }

}