package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import com.portal.conecta.comunicados.module.comunicado.application.query.GetAnnouncementByIdQuery;
import com.portal.conecta.comunicados.module.comunicado.application.query.ListAnnouncementHistoryQuery;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ListAnnouncementHistoryUseCase {

    private final GetAnnouncementByIdUseCase getAnnouncementByIdUseCase;
    private final AnnouncementHistoryRepository historyRepository;

    @Transactional(readOnly = true)
    public Page<AnnouncementHistory> execute(ListAnnouncementHistoryQuery query) {
        Announcement announcement = getAnnouncementByIdUseCase.execute(
                new GetAnnouncementByIdQuery(query.announcementId(), query.viewerUserId())
        );

        return historyRepository.findByAnnouncementIdOrderByCreatedAtDesc(
                announcement.getId(),
                query.toPageRequest()
        );
    }

}