package com.portal.conecta.comunicados.module.comunicado.domain.port.support;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementIndividualNotice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnnouncementIndividualNoticeRepository extends JpaRepository<AnnouncementIndividualNotice, UUID> {

    List<AnnouncementIndividualNotice> findByAnnouncementId(UUID announcementId);
}