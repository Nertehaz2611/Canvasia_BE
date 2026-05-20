package com.example.canvasia.service.interfaces;

import com.example.canvasia.dto.follow.FollowStatusResponse;
import com.example.canvasia.dto.follow.FollowUserFeedResponse;

public interface FollowService {

    FollowStatusResponse follow(String followerUsername, String followingUsername);

    FollowStatusResponse unfollow(String followerUsername, String followingUsername);

    FollowUserFeedResponse getFollowers(String username, int page, int size);

    FollowUserFeedResponse getFollowing(String username, int page, int size);
}
