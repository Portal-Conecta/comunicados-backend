package com.portal.conecta.comunicados.module.comunicado.presentation.mapper;

import com.portal.conecta.comunicados.module.comunicado.application.command.AttachAnnouncementFileCommand;
import com.portal.conecta.comunicados.module.comunicado.application.command.RemoveAnnouncementFileCommand;
import com.portal.conecta.comunicados.module.comunicado.application.command.SetAnnouncementThumbnailCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementFileRequest;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.response.AnnouncementFileResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AnnouncementFileMapper {

    @Mapping(source = "announcement.id", target = "announcementId")
    @Mapping(source = "thumbnail", target = "isThumbnail")
    AnnouncementFileResponse toResponse(AnnouncementFile entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "originalName", source = "originalName")
    @Mapping(target = "s3Key", source = "s3Key")
    @Mapping(target = "s3Bucket", source = "s3Bucket")
    @Mapping(target = "contentType", source = "contentType")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "sizeBytes", source = "sizeBytes")
    @Mapping(target = "isThumbnail", source = "isThumbnail")
    AnnouncementFile toEntity(CreateAnnouncementFileRequest request);

    default AttachAnnouncementFileCommand toAttachCommand(
            CreateAnnouncementFileRequest request,
            UUID uploadedByUserId
    ) {
        return new AttachAnnouncementFileCommand(request, uploadedByUserId);
    }

    default RemoveAnnouncementFileCommand toRemoveCommand(UUID fileId, UUID actorUserId) {
        return new RemoveAnnouncementFileCommand(fileId, actorUserId);
    }

    default SetAnnouncementThumbnailCommand toSetThumbnailCommand(
            UUID announcementId,
            UUID fileId,
            UUID actorUserId
    ) {
        return new SetAnnouncementThumbnailCommand(announcementId, fileId, actorUserId);
    }

}