package com.example.canvasia.service.interfaces;

import com.example.canvasia.dto.discover.LatestDiscussionFeedResponse;
import com.example.canvasia.dto.discover.LatestHashtagResponse;
import com.example.canvasia.dto.post.CursorPostFeedResponse;
import com.example.canvasia.dto.post.CursorThumbnailFeedResponse;

public interface DiscoverService {

    CursorPostFeedResponse getPostFeed(int limit, String cursor, String tag, String viewerUsername);

    CursorThumbnailFeedResponse getThumbnailFeed(int limit, String cursor);

    LatestDiscussionFeedResponse getLatestDiscussions(int limit);

    LatestHashtagResponse getLatestHashtags(int limit);
}
