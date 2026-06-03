package com.portal.conecta.comunicados.module.comunicado.presentation.mapper;

import com.portal.conecta.comunicados.module.comunicado.application.command.RecordAnnouncementHistoryCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementHistoryRequest;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.response.AnnouncementHistoryResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AnnouncementHistoryMapper {

    @Mapping(source = "announcement.id", target = "announcementId")
    AnnouncementHistoryResponse toResponse(AnnouncementHistory entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "action", source = "action")
    @Mapping(target = "snapshot", source = "snapshot")
    AnnouncementHistory toEntity(CreateAnnouncementHistoryRequest request);

    default RecordAnnouncementHistoryCommand toRecordCommand(CreateAnnouncementHistoryRequest request) {
        return new RecordAnnouncementHistoryCommand(request);
    }
    
}