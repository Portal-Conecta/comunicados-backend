package com.portal.conecta.comunicados.module.comunicado.domain.port.storage;

/**
 * Resultado de upload síncrono.
 *
 * @param awaitsAsyncProcessing {@code true} quando uma Lambda/processamento externo
 *        deve promover o arquivo a READY (fluxo S3); {@code false} em mock/local.
 */
public record StorageUploadResult(String s3Key, String s3Bucket, boolean awaitsAsyncProcessing) {

    public static StorageUploadResult async(String s3Key, String s3Bucket) {
        return new StorageUploadResult(s3Key, s3Bucket, true);
    }

    public static StorageUploadResult sync(String s3Key, String s3Bucket) {
        return new StorageUploadResult(s3Key, s3Bucket, false);
    }
}
