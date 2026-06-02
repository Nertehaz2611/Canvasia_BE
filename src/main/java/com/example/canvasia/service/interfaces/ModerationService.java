package com.example.canvasia.service.interfaces;

import java.util.UUID;

import com.example.canvasia.dto.moderation.ModerationCallbackRequest;

public interface ModerationService {

    void handleCallback(ModerationCallbackRequest request);

    void approvePendingPost(UUID postId, String adminUsername);

    void deletePendingPost(UUID postId, String adminUsername);
}