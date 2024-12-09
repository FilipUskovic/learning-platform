package com.micro.learningplatform.cache;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
public class CacheHealthIndicator extends AbstractHealthIndicator {

    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;

    public CacheHealthIndicator(CacheManager cacheManager, MeterRegistry meterRegistry) {
        super("Cache health check");
        this.cacheManager = cacheManager;
        this.meterRegistry = meterRegistry;
    }


    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        try {
            Map<String, Object> details = new HashMap<>();
            Collection<String> cacheNames = cacheManager.getCacheNames();

            // Skupljamo statistike za svaki cache
            cacheNames.forEach(cacheName -> {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache instanceof CaffeineCache caffeineCache) {
                    details.put(cacheName, getCacheStats(caffeineCache));
                }
            });

            builder.up()
                    .withDetails(details);

        } catch (Exception e) {
            builder.down()
                    .withException(e);
        }
    }

    private Map<String, Object> getCacheStats(CaffeineCache cache) {
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                cache.getNativeCache();
        CacheStats stats = nativeCache.stats();

        return Map.of(
                "size", nativeCache.estimatedSize(),
                "hitRate", stats.hitRate(),
                "missRate", stats.missRate(),
                "evictionCount", stats.evictionCount(),
                "averageLoadPenalty", stats.averageLoadPenalty()
        );
    }
}
