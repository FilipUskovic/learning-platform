package com.micro.learningplatform.cache;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CacheEventListener {

    private final MeterRegistry meterRegistry;


    public void onCacheEvent(CacheEvent event) {
        // Bilježimo događaj u metrike
        meterRegistry.counter("cache.events",
                "type", event.type().name(),
                "cache", event.cacheName()
        ).increment();

        // Bilježimo detaljne informacije u log
        log.debug("Cache event: {} for cache: {}, key: {}",
                event.type(),
                event.cacheName(),
                event.key());

        // Posebno pratimo eviction događaje
        if (event.type() == CacheEvent.CacheEventType.EVICTION) {
            handleEviction(event);
        }
    }

    private void handleEviction(CacheEvent event) {
        log.warn("Cache eviction occurred: cache={}, key={}, reason={}",
                event.cacheName(),
                event.key(),
                event.evictionReason().orElse("unknown"));

        meterRegistry.counter("cache.evictions",
                "cache", event.cacheName(),
                "reason", event.evictionReason().orElse("unknown")
        ).increment();
    }


}
