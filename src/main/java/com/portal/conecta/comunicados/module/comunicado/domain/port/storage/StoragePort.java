package com.portal.conecta.comunicados.module.comunicado.domain.port.storage;

public interface StoragePort {

    StorageUploadResult upload(String contentType, byte[] content);

    void delete(String s3Key);
}
