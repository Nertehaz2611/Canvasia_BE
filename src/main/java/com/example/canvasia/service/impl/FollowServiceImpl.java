package com.example.canvasia.service.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.canvasia.dto.follow.FollowStatusResponse;
import com.example.canvasia.dto.follow.FollowUserFeedResponse;
import com.example.canvasia.dto.follow.FollowUserItemResponse;
import com.example.canvasia.entity.Follow;
import com.example.canvasia.entity.Profile;
import com.example.canvasia.entity.User;
import com.example.canvasia.repository.FollowRepository;
import com.example.canvasia.repository.ProfileRepository;
import com.example.canvasia.repository.UserRepository;
import com.example.canvasia.service.interfaces.FollowService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final ProfileRepository profileRepository;

    @Override
    @Transactional
    public FollowStatusResponse follow(String followerUsername, String followingUsername) {
        User follower = getUserByUsername(followerUsername);
        User following = getUserByUsername(followingUsername);

        if (!followRepository.existsByFollowerUsernameAndFollowingUsername(followerUsername, followingUsername)) {
            Follow follow = Follow.create(follower, following);
            followRepository.save(follow);
        }

        return buildFollowStatus(followerUsername, following);
    }

    @Override
    @Transactional
    public FollowStatusResponse unfollow(String followerUsername, String followingUsername) {
        User following = getUserByUsername(followingUsername);

        followRepository.findByFollowerUsernameAndFollowingUsername(followerUsername, followingUsername)
                .ifPresent(followRepository::delete);

        return buildFollowStatus(followerUsername, following);
    }

    @Override
    @Transactional(readOnly = true)
    public FollowUserFeedResponse getFollowers(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Follow> followPage = followRepository.findByFollowingUsernameOrderByCreatedAtDesc(username, pageable);
        List<User> users = followPage.getContent().stream()
                .map(Follow::getFollower)
                .toList();

        return new FollowUserFeedResponse(
                toFollowUsers(users),
                followPage.getNumber(),
                followPage.getSize(),
                followPage.hasNext()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public FollowUserFeedResponse getFollowing(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Follow> followPage = followRepository.findByFollowerUsernameOrderByCreatedAtDesc(username, pageable);
        List<User> users = followPage.getContent().stream()
                .map(Follow::getFollowing)
                .toList();

        return new FollowUserFeedResponse(
                toFollowUsers(users),
                followPage.getNumber(),
                followPage.getSize(),
                followPage.hasNext()
        );
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private FollowStatusResponse buildFollowStatus(String viewerUsername, User target) {
        long followerCount = followRepository.countByFollowingUsername(target.getUsername());
        long followingCount = followRepository.countByFollowerUsername(target.getUsername());
        boolean isFollowing = viewerUsername != null
                && followRepository.existsByFollowerUsernameAndFollowingUsername(viewerUsername, target.getUsername());

        return new FollowStatusResponse(target.getUsername(), followerCount, followingCount, isFollowing);
    }

    private List<FollowUserItemResponse> toFollowUsers(Collection<User> users) {
        List<UUID> userIds = users.stream().map(User::getId).toList();
        if (userIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, Profile> profileByUserId = profileRepository.findByUserIdIn(userIds)
                .stream()
                .filter(profile -> profile.getUser() != null)
                .collect(Collectors.toMap(profile -> profile.getUser().getId(), Function.identity()));

        return users.stream()
                .map(user -> {
                    Profile profile = profileByUserId.get(user.getId());
                    return new FollowUserItemResponse(
                            user.getId(),
                            user.getUsername(),
                            user.getDisplayName(),
                            profile != null ? profile.getAvatarUrl() : null
                    );
                })
                .toList();
    }
}
