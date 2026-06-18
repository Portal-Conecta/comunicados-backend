package com.portal.conecta.comunicados.module.comunicado.infrastructure.storage;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StoragePort;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StorageUploadResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "storage", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class LocalStorageAdapter implements StoragePort {

    private static final String LOCAL_BUCKET = "local-storage";

    @Override
    public StorageUploadResult upload(String contentType, byte[] content) {
        String s3Key = UUID.randomUUID().toString();
        log.info("LocalStorageAdapter: upload simulado — key={}, contentType={}, size={}B",
                s3Key, contentType, content.length);
        return new StorageUploadResult(s3Key, LOCAL_BUCKET);
    }

    @Override
    public void delete(String s3Key) {
        log.info("LocalStorageAdapter: delete simulado — key={}", s3Key);
    }
}
