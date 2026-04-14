package com.example.canvasia.service.impl;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.canvasia.dto.post.CursorPostFeedResponse;
import com.example.canvasia.dto.post.CursorThumbnailFeedResponse;
import com.example.canvasia.dto.post.ThumbnailItemResponse;
import com.example.canvasia.entity.MediaVariant;
import com.example.canvasia.enums.MediaVariantType;
import com.example.canvasia.repository.MediaVariantRepository;
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

    private final MediaVariantRepository mediaVariantRepository;
    private final PostFeedAssembler postFeedAssembler;
    private final PostQueryService postQueryService;
    private final DiscoverCursorCodec discoverCursorCodec;

    @Override
    @Transactional(readOnly = true)
    public CursorPostFeedResponse getPostFeed(int limit, String cursor, String tag) {
        return postQueryService.getDiscoverPostsByCursor(limit, cursor, tag);
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
}
