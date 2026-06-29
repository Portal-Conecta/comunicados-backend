package com.portal.conecta.comunicados.module.comunicado.infrastructure.storage;

import java.util.Map;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.domain.port.presign.PresignedUpload;
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
        String s3Key = "comunicados/" + UUID.randomUUID();
        log.info("LocalStorageAdapter: upload simulado — key={}, contentType={}, size={}B",
                s3Key, contentType, content.length);
        return new StorageUploadResult(s3Key, LOCAL_BUCKET);
    }

    @Override
    public void delete(String s3Key, String s3Bucket) {
        log.info("LocalStorageAdapter: delete simulado — key={}, bucket={}", s3Key, s3Bucket);
    }

    @Override
    public PresignedUpload presignUpload(String s3Key, String contentType, long maxBytes) {
        log.info("LocalStorageAdapter: presignUpload simulado — key={}, contentType={}, maxBytes={}",
                s3Key, contentType, maxBytes);
        return new PresignedUpload("http://localhost/mock-presign/" + s3Key, Map.of(), s3Key, LOCAL_BUCKET);
    }
}
