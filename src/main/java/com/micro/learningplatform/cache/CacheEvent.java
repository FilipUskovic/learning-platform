package com.micro.learningplatform.cache;

import java.time.LocalDateTime;
import java.util.Optional;

public record CacheEvent(
        String cacheName,
        Object key,
        CacheEventType type,
        Optional<String> evictionReason,
        LocalDateTime timestamp

) {
    public enum CacheEventType {
        HIT, MISS, PUT, EVICTION
    }

    //TODO dodati validaciju da podaci nisu prazni
    public CacheEvent(String cacheName, Object key, CacheEventType type, Optional<String> evictionReason) {
        this(cacheName, key, type, evictionReason, LocalDateTime.now());
    }


}
