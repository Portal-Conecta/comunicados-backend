package com.portal.conecta.comunicados.module.comunicado.presentation.mapper;

import com.portal.conecta.comunicados.module.comunicado.application.command.CreateAnnouncementMentionCommand;
import com.portal.conecta.comunicados.module.comunicado.application.command.RemoveAnnouncementMentionCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementMention;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementMentionRequest;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.response.AnnouncementMentionResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AnnouncementMentionMapper {

    @Mapping(source = "announcement.id", target = "announcementId")
    AnnouncementMentionResponse toResponse(AnnouncementMention entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "userId", source = "userId")
    AnnouncementMention toEntity(CreateAnnouncementMentionRequest request);

    default CreateAnnouncementMentionCommand toCreateCommand(CreateAnnouncementMentionRequest request) {
        return new CreateAnnouncementMentionCommand(request);
    }

    default RemoveAnnouncementMentionCommand toRemoveCommand(
            UUID announcementId,
            UUID userId,
            UUID actorUserId
    ) {
        return new RemoveAnnouncementMentionCommand(announcementId, userId, actorUserId);
    }
    
}