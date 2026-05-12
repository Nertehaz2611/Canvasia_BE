package com.example.canvasia.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.canvasia.dto.discover.LatestDiscussionFeedResponse;
import com.example.canvasia.dto.discover.LatestDiscussionResponse;
import com.example.canvasia.dto.discover.LatestHashtagResponse;
import com.example.canvasia.dto.post.CursorPostFeedResponse;
import com.example.canvasia.dto.post.CursorThumbnailFeedResponse;
import com.example.canvasia.dto.post.ThumbnailItemResponse;
import com.example.canvasia.entity.Comment;
import com.example.canvasia.entity.MediaVariant;
import com.example.canvasia.entity.Profile;
import com.example.canvasia.enums.MediaVariantType;
import com.example.canvasia.enums.TagType;
import com.example.canvasia.repository.CommentRepository;
import com.example.canvasia.repository.MediaVariantRepository;
import com.example.canvasia.repository.ProfileRepository;
import com.example.canvasia.repository.PostTagRepository;
import com.example.canvasia.service.impl.post.PostFeedAssembler;
import com.example.canvasia.service.impl.post.PostQueryService;
import com.example.canvasia.service.impl.support.DiscoverCursorCodec;
import static com.example.canvasia.service.impl.support.PagingUtils.clampPageSize;
import com.example.canvasia.service.interfaces.DiscoverService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DiscoverServiceImpl implements DiscoverService {

    private static final int MAX_THUMBNAIL_PAGE_SIZE = 50;
    private static final int MAX_LATEST_DISCUSSION_SIZE = 20;
    private static final int MAX_LATEST_HASHTAG_SIZE = 20;

    private final MediaVariantRepository mediaVariantRepository;
    private final PostFeedAssembler postFeedAssembler;
    private final PostQueryService postQueryService;
    private final DiscoverCursorCodec discoverCursorCodec;
    private final CommentRepository commentRepository;
    private final PostTagRepository postTagRepository;
    private final ProfileRepository profileRepository;

    @Override
    @Transactional(readOnly = true)
    public CursorPostFeedResponse getPostFeed(int limit, String cursor, String tag, String viewerUsername) {
        return postQueryService.getDiscoverPostsByCursor(limit, cursor, tag, viewerUsername);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorThumbnailFeedResponse getThumbnailFeed(int limit, String cursor) {
        int safeLimit = clampPageSize(limit, MAX_THUMBNAIL_PAGE_SIZE);
        DiscoverCursorCodec.DecodedThumbnailCursor decodedCursor = discoverCursorCodec.decodeThumbnailCursor(cursor);

        List<MediaVariant> rows;
        if (decodedCursor.postCreatedAt() == null || decodedCursor.mediaId() == null) {
            rows = mediaVariantRepository.findThumbnailDiscoverFirstPage(
                MediaVariantType.THUMBNAIL,
                PageRequest.of(0, safeLimit + 1)
            );
        } else {
            rows = mediaVariantRepository.findThumbnailDiscoverSlice(
                MediaVariantType.THUMBNAIL,
                decodedCursor.postCreatedAt(),
                decodedCursor.mediaId(),
                PageRequest.of(0, safeLimit + 1)
            );
        }

        boolean hasNext = rows.size() > safeLimit;
        List<MediaVariant> itemsSlice = hasNext ? rows.subList(0, safeLimit) : rows;

        List<ThumbnailItemResponse> items = itemsSlice.stream()
                .map(postFeedAssembler::toThumbnailItemResponse)
                .toList();

        String nextCursor = hasNext ? discoverCursorCodec.encodeThumbnailCursor(itemsSlice.get(itemsSlice.size() - 1)) : null;

        return new CursorThumbnailFeedResponse(items, safeLimit, nextCursor, hasNext);
    }

    @Override
    @Transactional(readOnly = true)
    public LatestDiscussionFeedResponse getLatestDiscussions(int limit) {
        int safeLimit = clampPageSize(limit, MAX_LATEST_DISCUSSION_SIZE);
        List<Comment> comments = commentRepository.findLatestComments(PageRequest.of(0, safeLimit));
        Map<UUID, String> avatarUrlByUserId = loadAvatarUrls(comments);
        List<LatestDiscussionResponse> items = comments.stream()
                .map(comment -> new LatestDiscussionResponse(
                        comment.getId(),
                        comment.getPost().getId(),
                        comment.getUser().getId(),
                        comment.getUser().getDisplayName(),
                        comment.getUser().getUsername(),
                avatarUrlByUserId.get(comment.getUser().getId()),
                        comment.getContent(),
                        comment.getCreatedAt()
                ))
                .toList();
        return new LatestDiscussionFeedResponse(items);
    }

    @Override
    @Transactional(readOnly = true)
    public LatestHashtagResponse getLatestHashtags(int limit) {
        int safeLimit = clampPageSize(limit, MAX_LATEST_HASHTAG_SIZE);
        List<String> tags = postTagRepository.findLatestTagNames(TagType.HASHTAG, PageRequest.of(0, safeLimit));
        return new LatestHashtagResponse(tags);
    }

    private Map<UUID, String> loadAvatarUrls(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return Map.of();
        }

        Set<UUID> userIds = comments.stream()
                .map(comment -> comment.getUser().getId())
                .collect(Collectors.toSet());

        Map<UUID, String> avatarUrlByUserId = new HashMap<>();
        for (Profile profile : profileRepository.findByUserIdIn(userIds)) {
            if (profile.getUser() == null) {
                continue;
            }
            avatarUrlByUserId.put(profile.getUser().getId(), profile.getAvatarUrl());
        }
        return avatarUrlByUserId;
    }
}
