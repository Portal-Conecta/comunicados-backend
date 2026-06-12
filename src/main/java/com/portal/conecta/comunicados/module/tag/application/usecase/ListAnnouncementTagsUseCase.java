package com.portal.conecta.comunicados.module.tag.application.usecase;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementTag;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.AnnouncementTagRepository;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import com.portal.conecta.comunicados.shared.exception.UnauthorizedUserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ListAnnouncementTagsUseCase {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementTagRepository announcementTagRepository;
    private final RequestContextProvider requestContextProvider;

    public List<AnnouncementTag> execute(UUID announcementId) {
        announcementRepository.findById(announcementId)
                .orElseThrow(() -> new UnauthorizedUserException(
                        "Comunicado não encontrado ou sem permissão de acesso."
                ));

        return announcementTagRepository.findByAnnouncementId(announcementId);
    }
}
