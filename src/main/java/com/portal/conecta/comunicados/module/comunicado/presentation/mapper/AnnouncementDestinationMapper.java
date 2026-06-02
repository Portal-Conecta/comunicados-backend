package com.portal.conecta.comunicados.module.comunicado.presentation.mapper;

import java.util.List;
import java.util.UUID;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.portal.conecta.comunicados.module.comunicado.application.command.AddAnnouncementDestinationCommand;
import com.portal.conecta.comunicados.module.comunicado.application.command.RemoveAnnouncementDestinationCommand;
import com.portal.conecta.comunicados.module.comunicado.application.command.ReplaceAnnouncementDestinationsCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementDestinationRequest;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.response.AnnouncementDestinationResponse;

@Mapper(componentModel = "spring")
public interface AnnouncementDestinationMapper {

    @Mapping(source = "announcement.id", target = "announcementId")
    AnnouncementDestinationResponse toResponse(AnnouncementDestination entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "type", source = "type")
    @Mapping(target = "referenceId", source = "referenceId")
    AnnouncementDestination toEntity(CreateAnnouncementDestinationRequest request);

    default AddAnnouncementDestinationCommand toAddCommand(CreateAnnouncementDestinationRequest request) {
        return new AddAnnouncementDestinationCommand(request);
    }

    default RemoveAnnouncementDestinationCommand toRemoveCommand(UUID destinationId, UUID actorUserId) {
        return new RemoveAnnouncementDestinationCommand(destinationId, actorUserId);
    }

    default ReplaceAnnouncementDestinationsCommand toReplaceCommand(
        UUID announcementId,
        List<CreateAnnouncementDestinationRequest> destinations,
        UUID actorUserId
    ) {
        return new ReplaceAnnouncementDestinationsCommand(announcementId, destinations, actorUserId);
    }

}