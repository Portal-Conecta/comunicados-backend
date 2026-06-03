package com.portal.conecta.comunicados.module.comunicado.presentation.mapper;

import com.portal.conecta.comunicados.module.comunicado.application.command.CreateAnnouncementIndividualNoticeCommand;
import com.portal.conecta.comunicados.module.comunicado.application.command.ResolveAnnouncementIndividualNoticeCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementIndividualNotice;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementIndividualNoticeRequest;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.ResolveAnnouncementIndividualNoticeRequest;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.response.AnnouncementIndividualNoticeResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AnnouncementIndividualNoticeMapper {

    @Mapping(source = "announcement.id", target = "announcementId")
    @Mapping(source = "category.id", target = "categoryId")
    AnnouncementIndividualNoticeResponse toResponse(AnnouncementIndividualNotice entity);

    @BeanMapping(ignoreByDefault = true)
    AnnouncementIndividualNotice toEntity(CreateAnnouncementIndividualNoticeRequest request);

    default CreateAnnouncementIndividualNoticeCommand toCreateCommand(
            CreateAnnouncementIndividualNoticeRequest request,
            UUID actorUserId
    ) {
        return new CreateAnnouncementIndividualNoticeCommand(request, actorUserId);
    }

    default ResolveAnnouncementIndividualNoticeCommand toResolveCommand(
            UUID id,
            ResolveAnnouncementIndividualNoticeRequest request,
            UUID actorUserId
    ) {
        return new ResolveAnnouncementIndividualNoticeCommand(id, request, actorUserId);
    }
    
}