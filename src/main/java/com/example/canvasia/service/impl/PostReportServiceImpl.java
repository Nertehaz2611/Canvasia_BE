package com.example.canvasia.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.canvasia.dto.post.CreatePostReportRequest;
import com.example.canvasia.entity.Post;
import com.example.canvasia.entity.PostReport;
import com.example.canvasia.entity.User;
import com.example.canvasia.enums.ReportReason;
import com.example.canvasia.exception.DomainValidationException;
import com.example.canvasia.repository.PostRepository;
import com.example.canvasia.repository.PostReportRepository;
import com.example.canvasia.repository.UserRepository;
import com.example.canvasia.service.interfaces.PostReportService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostReportServiceImpl implements PostReportService {

    private final PostReportRepository postReportRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void reportPost(String reporterUsername, UUID postId, CreatePostReportRequest request) {
        User reporter = userRepository.findByUsername(reporterUsername)
                .orElseThrow(() -> new DomainValidationException("USER_NOT_FOUND", "User not found"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new DomainValidationException("POST_NOT_FOUND", "Post not found"));

        if (post.getUser().getUsername().equals(reporterUsername)) {
            throw new DomainValidationException("CANNOT_REPORT_OWN_POST", "You cannot report your own post");
        }

        if (postReportRepository.existsByReporterUsernameAndPostId(reporterUsername, postId)) {
            throw new DomainValidationException("ALREADY_REPORTED", "You have already reported this post");
        }

        List<ReportReason> reasons = request.reasons() != null ? request.reasons() : List.of();
        String otherReason = request.otherReason() != null ? request.otherReason().trim() : null;
        if (otherReason != null && otherReason.isEmpty()) {
            otherReason = null;
        }

        if (reasons.isEmpty() && (otherReason == null)) {
            throw new DomainValidationException("NO_REASON_PROVIDED", "At least one reason must be provided");
        }

        PostReport report = PostReport.create(reporter, post, reasons, otherReason);
        postReportRepository.save(report);
    }
}
