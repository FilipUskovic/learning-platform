package com.micro.learningplatform.config;

import lombok.Builder;

import java.time.Duration;

@Builder
public record CacheSpec(
        String name,
        long maxSize,
        Duration expireAfterWrite
) {
}
