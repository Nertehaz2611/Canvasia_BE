package com.example.canvasia.service.impl.post;

import com.example.canvasia.service.impl.PostServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostRetentionScheduler {

    private static final int RETENTION_DAYS = 30;

    private final PostServiceImpl postService;

    @Scheduled(cron = "0 0 2 * * *")
    public void purgeSoftDeletedPosts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int purged = postService.hardDeleteExpiredSoftDeletedPosts(cutoff);
        if (purged > 0) {
            log.info("Purged {} soft-deleted posts older than {} days", purged, RETENTION_DAYS);
        }
    }
}
