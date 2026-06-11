package com.portal.conecta.comunicados.module.comunicado.domain.specification;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AnnouncementSpecifications {

    private AnnouncementSpecifications() {
    }

    public static Specification<Announcement> notRemoved() {
        return (root, query, cb) -> cb.isNull(root.get("removedAt"));
    }

    public static Specification<Announcement> hasOrigin(AnnouncementOrigin origin) {
        return (root, query, cb) -> {
            if (origin == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("origin"), origin);
        };
    }

    public static Specification<Announcement> publishedBetween(Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("publishedAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("publishedAt"), to));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Announcement> hasDestinationClass(UUID classId) {
        return (root, query, cb) -> {
            if (classId == null) {
                return cb.conjunction();
            }
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<AnnouncementDestination> destination = subquery.from(AnnouncementDestination.class);
            subquery.select(destination.get("id"));
            subquery.where(cb.and(
                    cb.equal(destination.get("announcement"), root),
                    cb.equal(destination.get("type"), AnnouncementDestinationType.CLASS),
                    cb.equal(destination.get("referenceId"), classId)
            ));
            return cb.exists(subquery);
        };
    }

    public static Specification<Announcement> visibleTo(List<UUID> classIds, List<UUID> courseIds, UUID viewerUserId) {
        return (root, query, cb) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<AnnouncementDestination> destination = subquery.from(AnnouncementDestination.class);
            subquery.select(destination.get("id"));

            Predicate linked = cb.equal(destination.get("announcement"), root);

            Predicate general = cb.equal(destination.get("type"), AnnouncementDestinationType.GENERAL);

            Predicate classMatch = referenceMatch(cb, destination, AnnouncementDestinationType.CLASS, classIds);
            Predicate courseMatch = referenceMatch(cb, destination, AnnouncementDestinationType.COURSE, courseIds);

            Predicate userMatch = cb.and(
                    cb.equal(destination.get("type"), AnnouncementDestinationType.USER),
                    cb.equal(destination.get("referenceId"), viewerUserId)
            );

            subquery.where(cb.and(linked, cb.or(general, classMatch, courseMatch, userMatch)));
            return cb.exists(subquery);
        };
    }

    private static Predicate referenceMatch(
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Root<AnnouncementDestination> destination,
            AnnouncementDestinationType type,
            List<UUID> referenceIds
    ) {
        if (referenceIds == null || referenceIds.isEmpty()) {
            return cb.disjunction();
        }
        return cb.and(
                cb.equal(destination.get("type"), type),
                destination.get("referenceId").in(referenceIds)
        );
    }
}
