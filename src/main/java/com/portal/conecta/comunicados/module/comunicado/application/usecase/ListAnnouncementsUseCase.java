package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import com.portal.conecta.comunicados.module.comunicado.application.query.ListAnnouncementsQuery;
import com.portal.conecta.comunicados.module.comunicado.application.query.ListAnnouncementsResult;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementFileStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementPermissionDeniedException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementFileRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.hub.HubCoursePort;
import com.portal.conecta.comunicados.module.comunicado.domain.port.hub.HubShiftPort;
import com.portal.conecta.comunicados.module.comunicado.domain.port.storage.StoragePort;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubUserPort;
import com.portal.conecta.comunicados.module.comunicado.domain.specification.AnnouncementSpecifications;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.module.comunicado.infrastructure.storage.StorageProperties;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.PostFilterRequest;
import com.portal.conecta.comunicados.shared.context.ContextClass;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class ListAnnouncementsUseCase {

    private static final Sort PINNED_ORDER = Sort.by(Sort.Order.asc("pinnedOrder"));
    private static final Duration THUMBNAIL_URL_EXPIRY = Duration.ofHours(1);

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementFileRepository announcementFileRepository;
    private final RequestContextProvider contextProvider;
    private final AnnouncementPermissionValidator permissionValidator;
    private final HubCoursePort hubCoursePort;
    private final HubShiftPort hubShiftPort;
    private final HubUserPort hubUserPort;
    private final StoragePort storagePort;
    private final StorageProperties storageProperties;

    @Transactional
    public ListAnnouncementsResult execute(ListAnnouncementsQuery query) {
        RequestContext context = contextProvider.getRequestContext();
        Specification<Announcement> baseSpec = buildBaseSpecification(context, query.filter());

        List<Announcement> pinned = announcementRepository.findAll(
                baseSpec.and(AnnouncementSpecifications.isPinned()),
                PINNED_ORDER
        );

        Page<Announcement> items = announcementRepository.findAll(
                baseSpec.and(AnnouncementSpecifications.isNotPinned()),
                query.toPageRequest()
        );

        Map<UUID, String> thumbnailUrls = resolveThumbnailUrls(pinned, items.getContent());
        return new ListAnnouncementsResult(pinned, items, thumbnailUrls);
    }

    private Map<UUID, String> resolveThumbnailUrls(List<Announcement> pinned, List<Announcement> items) {
        List<UUID> announcementIds = Stream.concat(pinned.stream(), items.stream())
                .map(Announcement::getId)
                .distinct()
                .toList();

        if (announcementIds.isEmpty()) {
            return Map.of();
        }

        List<AnnouncementFile> thumbnails = announcementFileRepository
                .findThumbnailsByAnnouncementIdIn(announcementIds);

        Map<UUID, String> urls = new HashMap<>();
        for (AnnouncementFile thumbnail : thumbnails) {
            String url = resolveThumbnailUrl(thumbnail);
            if (url != null) {
                urls.put(thumbnail.getAnnouncement().getId(), url);
            }
        }
        return urls;
    }

    private String resolveThumbnailUrl(AnnouncementFile file) {
        if (file.getFileStatus() == AnnouncementFileStatus.PENDING) {
            reconcile(file);
        }
        if (file.getFileStatus() != AnnouncementFileStatus.READY) {
            return null;
        }
        if (file.getProcessedS3Key() != null) {
            return storagePort.presignDownload(
                    storageProperties.filesBucket(), file.getProcessedS3Key(), THUMBNAIL_URL_EXPIRY);
        }
        return storagePort.presignDownload(file.getS3Bucket(), file.getS3Key(), THUMBNAIL_URL_EXPIRY);
    }

    private void reconcile(AnnouncementFile file) {
        String processedKey = deriveProcessedKey(file.getS3Key());
        storagePort.headObject(storageProperties.filesBucket(), processedKey)
                .ifPresent(meta -> {
                    file.setFileStatus(AnnouncementFileStatus.READY);
                    file.setProcessedS3Key(processedKey);
                    file.setSizeBytes(meta.sizeBytes());
                    if (meta.contentType() != null && !meta.contentType().isBlank()) {
                        file.setContentType(meta.contentType());
                    }
                    announcementFileRepository.save(file);
                });
    }

    // "comunicados/{ownerId}/raw/{fileId}.ext" -> "comunicados/{ownerId}/processed/{fileId}"
    private String deriveProcessedKey(String rawKey) {
        String withoutExtension = rawKey.replaceAll("\\.[^./]+$", "");
        return withoutExtension.replace("/raw/", "/processed/");
    }

    private Specification<Announcement> buildBaseSpecification(RequestContext context, PostFilterRequest filter) {
        boolean mineRequested = filter.mineRequested();

        if (mineRequested && !permissionValidator.canCreate(context.userType())) {
            throw new AnnouncementPermissionDeniedException(
                    "Usuário não tem permissão para listar seus comunicados.");
        }

        Specification<Announcement> spec = AnnouncementSpecifications.notRemoved()
                .and(AnnouncementSpecifications.hasOrigin(filter.origin()))
                .and(AnnouncementSpecifications.publishedBetween(filter.publishedFrom(), filter.publishedTo()))
                .and(AnnouncementSpecifications.hasDestinationClass(filter.classId()));

        if (mineRequested) {
            // "Meus Comunicados": escopo por autoria. Inclui os agendados (SCHEDULED), então não
            // exige publicação; e ser autor dispensa a regra de visibilidade (visibleTo).
            spec = spec.and(AnnouncementSpecifications.createdBy(context.userId()));
        } else {
            spec = spec.and(AnnouncementSpecifications.isPublished());
        }

        if (hasSearch(filter)) {
            String searchTerm = filter.search().trim();
            List<UUID> matchingUserIds = hubUserPort.findUserIdsByNameContaining(searchTerm, context);
            spec = spec.and(AnnouncementSpecifications.matchesSearch(searchTerm, matchingUserIds));
        }

        List<UUID> tagIds = filter.resolvedTagIds();
        if (!tagIds.isEmpty()) {
            spec = spec.and(AnnouncementSpecifications.hasAnyTag(tagIds));
        }

        if (!mineRequested && !permissionValidator.canViewAll(context.userType())) {
            List<UUID> classes = classIds(context);
            spec = spec.and(AnnouncementSpecifications.visibleTo(
                    classes,
                    hubCoursePort.getCurrentUserCourseIds(),
                    context.userId(),
                    hubShiftPort.getShiftCodesForClasses(classes)
            ));
        }

        return spec;
    }

    private List<UUID> classIds(RequestContext context) {
        if (context.classes() == null) {
            return List.of();
        }
        return context.classes()
                .stream()
                .map(ContextClass::classId)
                .toList();
    }

    private boolean hasSearch(PostFilterRequest filter) {
        return filter.search() != null && !filter.search().isBlank();
    }
}
