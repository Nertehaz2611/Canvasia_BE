package com.example.canvasia.service.interfaces;

import com.example.canvasia.dto.post.CursorPostFeedResponse;
import com.example.canvasia.dto.post.CursorThumbnailFeedResponse;

public interface DiscoverService {

    CursorPostFeedResponse getPostFeed(int limit, String cursor, String tag);

    CursorThumbnailFeedResponse getThumbnailFeed(int limit, String cursor);
}
