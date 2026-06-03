package com.portal.conecta.comunicados.module.comunicado.domain.port;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileType;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnnouncementFileRepository extends JpaRepository<AnnouncementFile, UUID> {

    List<AnnouncementFile> findByAnnouncementId(UUID announcementId);

    long countByAnnouncementIdAndType(UUID announcementId, AnnouncementFileType type);
}