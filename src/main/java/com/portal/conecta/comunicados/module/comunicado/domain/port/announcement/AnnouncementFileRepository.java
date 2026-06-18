package com.portal.conecta.comunicados.module.comunicado.domain.port.announcement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileType;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;

public interface AnnouncementFileRepository extends JpaRepository<AnnouncementFile, UUID> {

    List<AnnouncementFile> findByAnnouncementId(UUID announcementId);

    long countByAnnouncementId(UUID announcementId);

    long countByAnnouncementIdAndType(UUID announcementId, AnnouncementFileType type);

    @Query("SELECT f FROM AnnouncementFile f WHERE f.announcement.id = :announcementId AND f.isThumbnail = true")
    Optional<AnnouncementFile> findThumbnailByAnnouncementId(@Param("announcementId") UUID announcementId);

    @Modifying
    @Query("UPDATE AnnouncementFile f SET f.isThumbnail = false WHERE f.announcement.id = :announcementId")
    void clearAllThumbnailsByAnnouncementId(@Param("announcementId") UUID announcementId);
}