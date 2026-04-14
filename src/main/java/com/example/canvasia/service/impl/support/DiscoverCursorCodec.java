package com.example.canvasia.service.impl.support;

import com.example.canvasia.entity.MediaVariant;
import com.example.canvasia.entity.Post;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

@Component
public class DiscoverCursorCodec {

    public String encodePostCursor(Post post) {
        long epochMillis = post.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli();
        return encode(epochMillis, post.getId());
    }

    public DecodedPostCursor decodePostCursor(String cursor) {
        DecodedRawCursor decoded = decode(cursor, "post");
        return new DecodedPostCursor(decoded.createdAt(), decoded.id());
    }

    public String encodeThumbnailCursor(MediaVariant variant) {
        long epochMillis = variant.getMedia().getPost().getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli();
        return encode(epochMillis, variant.getMedia().getId());
    }

    public DecodedThumbnailCursor decodeThumbnailCursor(String cursor) {
        DecodedRawCursor decoded = decode(cursor, "thumbnail");
        return new DecodedThumbnailCursor(decoded.createdAt(), decoded.id());
    }

    private String encode(long epochMillis, UUID id) {
        String raw = epochMillis + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private DecodedRawCursor decode(String cursor, String cursorName) {
        if (cursor == null || cursor.isBlank()) {
            return new DecodedRawCursor(null, null);
        }

        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid " + cursorName + " cursor format");
            }

            long epochMillis = Long.parseLong(parts[0]);
            LocalDateTime createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
            UUID id = UUID.fromString(parts[1]);

            return new DecodedRawCursor(createdAt, id);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid " + cursorName + " cursor value", ex);
        }
    }

    public record DecodedPostCursor(LocalDateTime createdAt, UUID id) {
    }

    public record DecodedThumbnailCursor(LocalDateTime postCreatedAt, UUID mediaId) {
    }

    private record DecodedRawCursor(LocalDateTime createdAt, UUID id) {
    }
}
