package com.portal.conecta.comunicados.module.comunicado.domain.port.storage;

import com.portal.conecta.comunicados.module.comunicado.domain.port.presign.PresignedUpload;

public interface StoragePort {

    StorageUploadResult upload(String contentType, byte[] content);

    void delete(String s3Key, String s3Bucket);

    PresignedUpload presignUpload(String s3Key, String contentType, long maxBytes);
}
