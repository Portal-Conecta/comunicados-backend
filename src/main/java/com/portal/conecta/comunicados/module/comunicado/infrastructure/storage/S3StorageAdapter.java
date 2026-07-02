package com.portal.conecta.comunicados.module.comunicado.infrastructure.storage;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.domain.port.presign.PresignedUpload;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StorageObjectMetadata;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StoragePort;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StorageUploadResult;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
@ConditionalOnProperty(prefix = "storage", name = "mock-enabled", havingValue = "false")
public class S3StorageAdapter implements StoragePort {

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
        String bucket = properties.imagesBucket();

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
     * Todo upload vai para o imagesBucket (bucket A): a Lambda triggerada lá processa
     * imagens raster (compressão para webp) e preserva os demais tipos como estão,
     * copiando ambos os casos para o filesBucket (bucket B) em {@code .../processed/{fileId}}.
     */
    @Override
    public PresignedUpload presignUpload(String s3Key, String contentType, long maxBytes) {
        String bucket = properties.imagesBucket();

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

    @Override
    public String presignDownload(String bucket, String key, Duration expiry) {
        return presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(expiry)
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build())
                        .build()
        ).url().toString();
    }

    @Override
    public Optional<StorageObjectMetadata> headObject(String bucket, String key) {
        try {
            HeadObjectResponse response = s3Client.headObject(r -> r.bucket(bucket).key(key));
            Map<String, String> meta = response.metadata();
            Integer width = parseIntOrNull(meta.get("width"));
            Integer height = parseIntOrNull(meta.get("height"));
            return Optional.of(new StorageObjectMetadata(response.contentLength(), width, height));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    private Integer parseIntOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return null; }
    }
}
