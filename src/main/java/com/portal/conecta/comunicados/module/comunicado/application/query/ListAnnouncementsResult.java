package com.portal.conecta.comunicados.module.comunicado.application.query;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ListAnnouncementsResult(

    List<Announcement> pinned,
    Page<Announcement> items,
    Map<UUID, String> thumbnailUrlsByAnnouncementId,
    Map<UUID, List<Tag>> tagsByAnnouncementId

) {
}
