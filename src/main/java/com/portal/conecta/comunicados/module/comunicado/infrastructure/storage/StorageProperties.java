package com.portal.conecta.comunicados.module.comunicado.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public record StorageProperties(

        boolean mockEnabled,
        int maxFilesPerAnnouncement,
        int maxFileSizeMb,

        int maxImageSizeMb,
        int maxDocumentSizeMb,
        int maxVideoSizeMb,

        String imagesBucket,
        String filesBucket,
        String region

) {

    public long maxFileSizeBytes() {
        return (long) maxFileSizeMb * 1024 * 1024;
    }

    public long maxImageSizeBytes() {
        return (long) maxImageSizeMb * 1024 * 1024;
    }

    public long maxDocumentSizeBytes() {
        return (long) maxDocumentSizeMb * 1024 * 1024;
    }

    public long maxVideoSizeBytes() {
        return (long) maxVideoSizeMb * 1024 * 1024;
    }
}
