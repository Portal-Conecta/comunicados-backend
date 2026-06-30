package com.portal.conecta.comunicados.module.comunicado.infrastructure.storage;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.domain.port.presign.PresignedUpload;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StoragePort;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StorageUploadResult;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
@ConditionalOnProperty(prefix = "storage", name = "mock-enabled", havingValue = "false")
public class S3StorageAdapter implements StoragePort {

    // Imagens que passam por processamento Lambda (bucket A)
    private static final Set<String> IMAGE_BUCKET_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final Duration PRESIGN_DURATION = Duration.ofMinutes(15);
    private static final String KEY_PREFIX = "comunicados/";

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final StorageProperties properties;

    public S3StorageAdapter(StorageProperties properties) {
        this.properties = properties;
        Region region = Region.of(properties.region());
        this.s3Client = S3Client.builder().region(region).build();
        this.presigner = S3Presigner.builder().region(region).build();
    }

    @Override
    public StorageUploadResult upload(String contentType, byte[] content) {
        String s3Key = KEY_PREFIX + UUID.randomUUID();
        String bucket = resolveBucket(contentType);

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(s3Key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(content)
        );

        return new StorageUploadResult(s3Key, bucket);
    }

    @Override
    public void delete(String s3Key, String s3Bucket) {
        s3Client.deleteObject(r -> r.bucket(s3Bucket).key(s3Key));
    }

    /**
     * Gera presigned PUT válido por 15 min, com Content-Type obrigatório na requisição.
     * Roteamento: jpeg/png/gif/webp → imagesBucket (Lambda); demais → filesBucket (direto).
     */
    @Override
    public PresignedUpload presignUpload(String s3Key, String contentType, long maxBytes) {
        String bucket = resolveBucket(contentType);

        PresignedPutObjectRequest presigned = presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(PRESIGN_DURATION)
                        .putObjectRequest(PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(s3Key)
                                .contentType(contentType)
                                .build())
                        .build()
        );

        return new PresignedUpload(presigned.url().toString(), Map.of(), s3Key, bucket);
    }

    private String resolveBucket(String contentType) {
        return IMAGE_BUCKET_TYPES.contains(contentType)
                ? properties.imagesBucket()
                : properties.filesBucket();
    }
}
