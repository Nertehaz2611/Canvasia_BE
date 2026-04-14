package com.example.canvasia.service.impl.support;

public final class PagingUtils {

    private PagingUtils() {
    }

    public static int clampPageSize(int requestedSize, int maxSize) {
        if (requestedSize <= 0) {
            return maxSize;
        }
        return Math.min(requestedSize, maxSize);
    }
}
