package com.example.canvasia.controller.support;

import com.example.canvasia.dto.post.CreatePostRequest;
import com.example.canvasia.dto.post.UpdatePostRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostRequestResolver {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Validator validator;

    public CreatePostRequest resolveCreateRequest(String payload, String caption, List<String> tags) {
        CreatePostRequest request = parseJson(payload, CreatePostRequest.class);
        if (request != null) {
            validate(request);
            return request;
        }

        if (!hasBasicFields(caption, tags)) {
            return null;
        }

        CreatePostRequest fallback = new CreatePostRequest(caption, normalizeTags(tags), List.of());
        validate(fallback);
        return fallback;
    }

    public UpdatePostRequest resolveUpdateRequest(String payload, String caption, List<String> tags) {
        UpdatePostRequest request = parseJson(payload, UpdatePostRequest.class);
        if (request != null) {
            validate(request);
            return request;
        }

        if (!hasBasicFields(caption, tags)) {
            return null;
        }

        UpdatePostRequest fallback = new UpdatePostRequest(caption, normalizeTags(tags), List.of(), List.of(), List.of());
        validate(fallback);
        return fallback;
    }

    private <T> T parseJson(String payload, Class<T> type) {
        if (payload == null || payload.isBlank()) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(payload, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("payload must be valid JSON");
        }
    }

    private boolean hasBasicFields(String caption, List<String> tags) {
        return (caption != null && !caption.isBlank()) || (tags != null && !tags.isEmpty());
    }

    private List<String> normalizeTags(List<String> tags) {
        return tags == null ? List.of() : tags;
    }

    private <T> void validate(T request) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException(message);
        }
    }
}
