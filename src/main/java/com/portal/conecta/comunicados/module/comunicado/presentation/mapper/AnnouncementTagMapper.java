package com.portal.conecta.comunicados.module.comunicado.presentation.mapper;

import com.portal.conecta.comunicados.module.comunicado.application.command.LinkAnnouncementTagCommand;
import com.portal.conecta.comunicados.module.comunicado.application.command.UnlinkAnnouncementTagCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementTag;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.LinkAnnouncementTagRequest;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.response.AnnouncementTagResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AnnouncementTagMapper {

    @Mapping(source = "announcement.id", target = "announcementId")
    @Mapping(source = "tag.id", target = "tagId")
    @Mapping(source = "tag.name", target = "tagName")
    AnnouncementTagResponse toResponse(AnnouncementTag entity);

    default LinkAnnouncementTagCommand toLinkCommand(LinkAnnouncementTagRequest request) {
        return new LinkAnnouncementTagCommand(request);
    }

    default UnlinkAnnouncementTagCommand toUnlinkCommand(
            UUID announcementId,
            UUID tagId,
            UUID actorUserId) {
        return new UnlinkAnnouncementTagCommand(announcementId, tagId, actorUserId);
    }

}