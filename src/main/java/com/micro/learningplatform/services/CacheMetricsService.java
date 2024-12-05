package com.micro.learningplatform.services;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheMetricsService {

    private final MeterRegistry meterRegistry;

    // ovo je servis koji prati metrike

    public void recordCacheHit(String cacheName) {
        meterRegistry.counter("cache.hit",
                "cache", cacheName).increment();
    }

    public void recordCacheMiss(String cacheName) {
        meterRegistry.counter("cache.miss",
                "cache", cacheName).increment();
    }

    public void recordCacheEviction(
            String cacheName,
            String key,
            String cause) {

        meterRegistry.counter("cache.eviction",
                "cache", cacheName,
                "cause", cause).increment();

        log.debug("Cache eviction: cache={}, key={}, cause={}",
                cacheName, key, cause);
    }
}
