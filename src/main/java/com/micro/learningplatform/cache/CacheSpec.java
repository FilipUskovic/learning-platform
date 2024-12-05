package com.micro.learningplatform.cache;

import lombok.Builder;

import java.time.Duration;

@Builder
public record CacheSpec(
        String name,
        long maxSize,
        Duration expireAfterWrite
) {
}
