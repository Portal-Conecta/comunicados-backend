package com.portal.conecta.comunicados.module.comunicado.presentation.mapper;

import com.portal.conecta.comunicados.module.comunicado.application.command.CreateAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.application.command.ScheduleAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.application.command.UpdateAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.response.AnnouncementResponse;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.response.AnnouncementSummaryResponse;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementRequest;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.ScheduleAnnouncementRequest;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.UpdateAnnouncementRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AnnouncementMapper {

    AnnouncementResponse toResponse(Announcement entity);

    AnnouncementSummaryResponse toSummary(Announcement entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "title", source = "title")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "origin", source = "origin")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "pinned", source = "pinned")
    @Mapping(target = "pinnedOrder", source = "pinnedOrder")
    @Mapping(target = "scheduledFor", source = "scheduledFor")
    Announcement toEntity(CreateAnnouncementRequest request);

    @BeanMapping(
            ignoreByDefault = true,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "title", source = "title")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "origin", source = "origin")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "pinned", source = "pinned")
    @Mapping(target = "pinnedOrder", source = "pinnedOrder")
    @Mapping(target = "scheduledFor", source = "scheduledFor")
    void applyUpdate(UpdateAnnouncementRequest request, @MappingTarget Announcement entity);

    default CreateAnnouncementCommand toCreateCommand(CreateAnnouncementRequest request, UUID createdByUserId) {
        return new CreateAnnouncementCommand(request, createdByUserId);
    }

    default UpdateAnnouncementCommand toUpdateCommand(UUID id, UpdateAnnouncementRequest request, UUID updatedByUserId) {
        return UpdateAnnouncementCommand.fromRequest(id, request, updatedByUserId);
    }

    default ScheduleAnnouncementCommand toScheduleCommand(UUID id, ScheduleAnnouncementRequest request) {
        return new ScheduleAnnouncementCommand(id, request);
    }

}