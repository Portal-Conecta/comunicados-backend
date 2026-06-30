package com.portal.conecta.comunicados.module.comunicado.application.query;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import org.springframework.data.domain.Page;

import java.util.List;

public record ListAnnouncementsResult(

    List<Announcement> pinned,
    Page<Announcement> items

) {
}
