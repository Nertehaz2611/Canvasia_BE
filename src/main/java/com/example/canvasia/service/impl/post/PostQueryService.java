package com.example.canvasia.service.impl.post;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.canvasia.dto.post.CursorPostFeedResponse;
import com.example.canvasia.dto.post.PostFeedResponse;
import com.example.canvasia.dto.post.PostResponse;
import com.example.canvasia.entity.Post;
import com.example.canvasia.repository.PostRepository;
import com.example.canvasia.service.impl.support.DiscoverCursorCodec;
import static com.example.canvasia.service.impl.support.PagingUtils.clampPageSize;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostQueryService {

    private static final int MAX_POST_PAGE_SIZE = 10;

    private final PostRepository postRepository;
    private final PostTagResolver postTagResolver;
    private final PostFeedAssembler postFeedAssembler;
    private final DiscoverCursorCodec discoverCursorCodec;

    @Transactional(readOnly = true)
    public CursorPostFeedResponse getDiscoverPostsByCursor(int limit, String cursor, String tag, String viewerUsername) {
        int safeLimit = clampPageSize(limit, MAX_POST_PAGE_SIZE);
        DiscoverCursorCodec.DecodedPostCursor decodedCursor = discoverCursorCodec.decodePostCursor(cursor);
        boolean hasCursor = decodedCursor.createdAt() != null && decodedCursor.id() != null;

        List<Post> rows;
        if (tag == null || tag.isBlank()) {
            if (hasCursor) {
                rows = postRepository.findDiscoverSlice(
                        decodedCursor.createdAt(),
                        decodedCursor.id(),
                        PageRequest.of(0, safeLimit + 1)
                );
            } else {
                rows = postRepository.findDiscoverFirstPage(PageRequest.of(0, safeLimit + 1));
            }
        } else {
            PostTagResolver.NormalizedTag normalizedTag = postTagResolver.parse(tag);
            String legacyTagName = stripPrefix(normalizedTag.name());
            String tagType = normalizedTag.type().name();
            if (hasCursor) {
                rows = postRepository.findDiscoverSliceByTag(
                        normalizedTag.name(),
                        legacyTagName,
                        tagType,
                        decodedCursor.createdAt(),
                        decodedCursor.id(),
                        PageRequest.of(0, safeLimit + 1)
                );
            } else {
                rows = postRepository.findDiscoverFirstPageByTag(
                        normalizedTag.name(),
                        legacyTagName,
                        tagType,
                        PageRequest.of(0, safeLimit + 1)
                );
            }
        }

        boolean hasNext = rows.size() > safeLimit;
        List<Post> itemsSlice = hasNext ? rows.subList(0, safeLimit) : rows;
        List<PostResponse> items = postFeedAssembler.toPostResponses(itemsSlice, viewerUsername);
        String nextCursor = hasNext ? discoverCursorCodec.encodePostCursor(itemsSlice.get(itemsSlice.size() - 1)) : null;

        return new CursorPostFeedResponse(items, safeLimit, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public PostFeedResponse getPostsByUser(String viewerUsername, String username, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = clampPageSize(size, MAX_POST_PAGE_SIZE);
        PageRequest pageable = buildPostPageRequest(safePage, safeSize);

        Page<Post> posts = postRepository.findByUserUsernameAndIsDeletedFalse(username, pageable);
        return new PostFeedResponse(postFeedAssembler.toPostResponses(posts.getContent(), viewerUsername), safePage, safeSize, posts.hasNext());
    }

    @Transactional(readOnly = true)
    public PostFeedResponse getArchivedPostsByOwner(String username, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = clampPageSize(size, MAX_POST_PAGE_SIZE);
        PageRequest pageable = buildPostPageRequest(safePage, safeSize);

        Page<Post> posts = postRepository.findByUserUsernameAndIsDeletedTrue(username, pageable);
        return new PostFeedResponse(postFeedAssembler.toPostResponses(posts.getContent(), username), safePage, safeSize, posts.hasNext());
    }

    @Transactional(readOnly = true)
    public PostFeedResponse getPostsByTag(String viewerUsername, String tag, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = clampPageSize(size, MAX_POST_PAGE_SIZE);
        PageRequest pageable = buildPostPageRequest(safePage, safeSize);

        PostTagResolver.NormalizedTag normalizedTag = postTagResolver.parse(tag);
        String legacyTagName = stripPrefix(normalizedTag.name());
        Page<Post> posts = postRepository.findByTag(
            normalizedTag.name(),
            legacyTagName,
            normalizedTag.type().name(),
            pageable
        );

        return new PostFeedResponse(postFeedAssembler.toPostResponses(posts.getContent(), viewerUsername), safePage, safeSize, posts.hasNext());
    }

    private PageRequest buildPostPageRequest(int safePage, int safeSize) {
        return PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
    }

    private String stripPrefix(String tagName) {
        if (tagName == null || tagName.isBlank()) {
            return tagName;
        }
        if (tagName.startsWith("#") || tagName.startsWith("@")) {
            return tagName.substring(1);
        }
        return tagName;
    }

}
