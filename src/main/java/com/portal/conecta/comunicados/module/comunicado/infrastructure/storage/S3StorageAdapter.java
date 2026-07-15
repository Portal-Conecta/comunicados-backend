package com.portal.conecta.comunicados.module.comunicado.infrastructure.storage;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

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
    public StorageUploadResult upload(String s3Key, String contentType, byte[] content) {
        String bucket = properties.imagesBucket();

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(s3Key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(content)
        );

        // Lambda no imagesBucket processa raw → processed no filesBucket.
        return StorageUploadResult.async(s3Key, bucket);
    }

    @Override
    public void delete(String s3Key, String s3Bucket) {
        s3Client.deleteObject(r -> r.bucket(s3Bucket).key(s3Key));
    }

    /**
     * Gera presigned PUT válido por 15 min, com Content-Type e Content-Length obrigatórios.
     * {@code contentLengthBytes} deve ser o tamanho declarado (já validado contra o teto);
     * o cliente só consegue fazer PUT com esse Content-Length exato.
     */
    @Override
    public PresignedUpload presignUpload(String s3Key, String contentType, long contentLengthBytes) {
        String bucket = properties.imagesBucket();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(PRESIGN_DURATION)
                        .putObjectRequest(PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(s3Key)
                                .contentType(contentType)
                                .contentLength(contentLengthBytes)
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
            return Optional.of(new StorageObjectMetadata(response.contentLength(), width, height, response.contentType()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    private Integer parseIntOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return null; }
    }
}
