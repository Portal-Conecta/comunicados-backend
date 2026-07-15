package com.portal.conecta.comunicados.module.comunicado.domain.enums;

import java.util.Map;
import java.util.Optional;

public enum AnnouncementFileType {

    IMAGE, DOCUMENT, VIDEO;

    private static final Map<String, AnnouncementFileType> BY_CONTENT_TYPE = Map.ofEntries(
            Map.entry("image/jpeg",       IMAGE),
            Map.entry("image/png",        IMAGE),
            Map.entry("image/gif",        IMAGE),
            Map.entry("image/webp",       IMAGE),
            Map.entry("application/pdf",  DOCUMENT),
            Map.entry("video/mp4",        VIDEO),
            Map.entry("video/webm",       VIDEO)
    );

    public static Optional<AnnouncementFileType> fromContentType(String contentType) {
        return Optional.ofNullable(BY_CONTENT_TYPE.get(contentType));
    }
}
