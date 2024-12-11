package com.micro.learningplatform.cache;

import com.micro.learningplatform.services.CacheMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheEventService {

    /* Ovaj servis je sada zadužen samo za centraliziranu obradu događaja
     */

    private final CacheMetricsService metricsService;

    public void handleCacheEvents(CacheEvent cacheEvent) {
        Map<String, String> tags = Map.of("cache", cacheEvent.cacheName(), "type", cacheEvent.type().name());
        metricsService.recordMetrics("cache.events", cacheEvent.cacheName(), tags);

        if (cacheEvent.type() == CacheEvent.CacheEventType.EVICTION) {
            handleEviction(cacheEvent);
        }
    }

    private void handleEviction(CacheEvent event) {
        Map<String, String> tags = Map.of("cache", event.cacheName(), "reason", event.evictionReason().orElse("unknown"));
        metricsService.recordMetrics("cache.evictions", event.cacheName(), tags);

        log.warn("Cache eviction: cache={}, key={}, reason={}", event.cacheName(), event.key(), event.evictionReason().orElse("unknown"));
    }

}
