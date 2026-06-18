package com.portal.conecta.comunicados.module.comunicado.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public record StorageProperties(

        boolean mockEnabled,
        int maxFilesPerAnnouncement,
        int maxFileSizeMb

) {

    public long maxFileSizeBytes() {
        return (long) maxFileSizeMb * 1024 * 1024;
    }
}
