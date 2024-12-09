package com.micro.learningplatform.cache;

import java.util.Optional;

public record CacheEvent(
        String cacheName,
        Object key,
        CacheEventType type,
        Optional<String> evictionReason
) {
    public enum CacheEventType {
        HIT, MISS, PUT, EVICTION
    }


}
