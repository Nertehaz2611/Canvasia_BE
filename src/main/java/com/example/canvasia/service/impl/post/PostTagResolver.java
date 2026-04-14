package com.example.canvasia.service.impl.post;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.canvasia.entity.Tag;
import com.example.canvasia.enums.TagType;
import com.example.canvasia.repository.TagRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostTagResolver {

    private static final Pattern TAG_BODY_PATTERN = Pattern.compile("^[a-z0-9._-]{1,50}$");

    private final TagRepository tagRepository;

    public List<Tag> resolve(List<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<NormalizedTag> normalizedTags = rawTags.stream()
                .map(this::parse)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Tag> resolved = new ArrayList<>();
        for (NormalizedTag normalizedTag : normalizedTags) {
            Tag tag = tagRepository.findByNameAndType(normalizedTag.name(), normalizedTag.type())
                    .orElseGet(() -> tagRepository.save(Tag.create(normalizedTag.name(), normalizedTag.type())));
            resolved.add(tag);
        }

        return resolved;
    }

    public NormalizedTag parse(String rawTag) {
        if (rawTag == null || rawTag.isBlank()) {
            throw new IllegalArgumentException("Tag must not be blank");
        }

        String decoded = URLDecoder.decode(rawTag.trim(), StandardCharsets.UTF_8);
        String normalized = decoded.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("#")) {
            return build(normalized.substring(1), TagType.HASHTAG, "#");
        }
        if (normalized.startsWith("@")) {
            return build(normalized.substring(1), TagType.USER_MENTION, "@");
        }

        return build(normalized, TagType.HASHTAG, "#");
    }

    private NormalizedTag build(String rawBody, TagType type, String prefix) {
        String body = rawBody.replaceAll("\\s+", "");
        if (!TAG_BODY_PATTERN.matcher(body).matches()) {
            throw new IllegalArgumentException("Tag contains invalid characters: " + prefix + rawBody);
        }
        return new NormalizedTag(prefix + body, type);
    }

    public record NormalizedTag(String name, TagType type) {
    }
}
