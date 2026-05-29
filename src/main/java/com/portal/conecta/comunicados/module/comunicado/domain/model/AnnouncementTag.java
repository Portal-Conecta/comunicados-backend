package com.portal.conecta.comunicados.module.comunicado.domain.model;

import com.portal.conecta.comunicados.module.tag.domain.model.Tag;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "announcement_tag")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementTag {

    @ManyToOne
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @ManyToOne
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

}
