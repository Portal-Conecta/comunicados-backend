package com.portal.conecta.comunicados.module.comunicado.domain.model;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "announcement_file")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "s3_bucket", nullable = false)
    private String s3Bucket;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "type", nullable = false)
    private AnnouncementFileType type;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "is_thumbnail", nullable = false)
    private boolean isThumbnail;

    @Column(name = "uploaded_by_user_id", nullable = false)
    private UUID uploadedByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
}
