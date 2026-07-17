package com.portal.conecta.comunicados.module.comunicado.infrastructure.storage;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.domain.port.presign.PresignedUpload;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StorageObjectMetadata;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StoragePort;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StorageUploadResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "storage", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class LocalStorageAdapter implements StoragePort {

    private static final String LOCAL_BUCKET = "local-storage";

    @Override
    public StorageUploadResult upload(String s3Key, String contentType, byte[] content) {
        log.info("LocalStorageAdapter: upload simulado — key={}, contentType={}, size={}B",
                s3Key, contentType, content.length);
        // Mock: disponível imediatamente (sem Lambda).
        return StorageUploadResult.sync(s3Key, LOCAL_BUCKET);
    }

    @Override
    public void delete(String s3Key, String s3Bucket) {
        log.info("LocalStorageAdapter: delete simulado — key={}, bucket={}", s3Key, s3Bucket);
    }

    @Override
    public PresignedUpload presignUpload(String s3Key, String contentType, long contentLengthBytes) {
        log.info("LocalStorageAdapter: presignUpload simulado — key={}, contentType={}, contentLength={}",
                s3Key, contentType, contentLengthBytes);
        return new PresignedUpload("http://localhost/mock-presign/" + s3Key, Map.of(), s3Key, LOCAL_BUCKET);
    }

    @Override
    public Optional<StorageObjectMetadata> headObject(String bucket, String key) {
        log.info("LocalStorageAdapter: headObject simulado — bucket={}, key={}", bucket, key);
        return Optional.empty();
    }

    @Override
    public String presignDownload(String bucket, String key, Duration expiry) {
        log.info("LocalStorageAdapter: presignDownload simulado — bucket={}, key={}", bucket, key);
        return "http://localhost/mock-download/" + key;
    }
}
