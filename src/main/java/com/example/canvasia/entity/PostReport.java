package com.example.canvasia.entity;

import java.util.List;

import com.example.canvasia.entity.base.AuditableEntity;
import com.example.canvasia.enums.ReportReason;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "post_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class PostReport extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false, updatable = false)
    @ToString.Exclude
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, updatable = false)
    @ToString.Exclude
    private Post post;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "post_report_reasons", joinColumns = @JoinColumn(name = "report_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "reason")
    private List<ReportReason> reasons;

    @Column(columnDefinition = "TEXT")
    private String otherReason;

    public static PostReport create(User reporter, Post post, List<ReportReason> reasons, String otherReason) {
        validate(reporter, post);
        return PostReport.builder()
                .reporter(reporter)
                .post(post)
                .reasons(reasons != null ? reasons : List.of())
                .otherReason(otherReason)
                .build();
    }
}
