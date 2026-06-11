package com.portal.conecta.comunicados.module.comunicado.application.query;

import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.PostFilterRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.UUID;

public record ListAnnouncementsQuery(

    PostFilterRequest filter,
    UUID viewerUserId

) {

    private static final Sort DEFAULT_SORT =
            Sort.by("publishedAt").descending().and(Sort.by("id").ascending());

    public Pageable toPageRequest() {
        return PageRequest.of(filter.page(), filter.size(), DEFAULT_SORT);
    }
}
