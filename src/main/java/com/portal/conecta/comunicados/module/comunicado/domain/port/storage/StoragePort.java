package com.portal.conecta.comunicados.module.comunicado.domain.port.storage;

import java.time.Duration;
import java.util.Optional;

import com.portal.conecta.comunicados.module.comunicado.domain.port.presign.PresignedUpload;

public interface StoragePort {

    /**
     * Upload direto (multipart). O caller define a {@code s3Key} (padrão raw do Lambda).
     */
    StorageUploadResult upload(String s3Key, String contentType, byte[] content);

    void delete(String s3Key, String s3Bucket);

    /**
     * Presigned PUT. {@code contentLengthBytes} é o tamanho que o cliente deve enviar
     * (Content-Length exato no PUT assinada).
     */
    PresignedUpload presignUpload(String s3Key, String contentType, long contentLengthBytes);

    Optional<StorageObjectMetadata> headObject(String bucket, String key);

    String presignDownload(String bucket, String key, Duration expiry);
}
