package com.micro.learningplatform.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CaffeineCacheConfigurationHelper {

    private final CacheEventListener cacheEventListener;
    private final MeterRegistry meterRegistry;


    public CustomCaffeineCacheManager createCaffeineCacheManager() {
        CustomCaffeineCacheManager cacheManager = new CustomCaffeineCacheManager(cacheEventListener);
        Map<String, Caffeine<Object, Object>> cacheBuilders = configureCaffeineCacheStrategies();
        cacheManager.setCacheBuilders(cacheBuilders);
        registerLocalCacheMetrics(cacheManager);
        return cacheManager;
    }

    private Map<String, Caffeine<Object, Object>> configureCaffeineCacheStrategies() {
        Map<String, Caffeine<Object, Object>> cacheBuilders = new HashMap<>();
        for (CacheStrategy strategy : CacheStrategy.values()) {
            cacheBuilders.put(strategy.getSpec().name(), configureCacheBuilder(strategy.getSpec()));
        }
        return cacheBuilders;
    }

    private Caffeine<Object, Object> configureCacheBuilder(CacheSpec spec) {
        return Caffeine.newBuilder()
                .maximumSize(spec.maxSize())
                .expireAfterWrite(spec.expireAfterWrite())
                .recordStats()
                .removalListener((key, value, cause) -> cacheEventListener.onCacheEvent(new CacheEvent(
                        spec.name(),
                        key,
                        CacheEvent.CacheEventType.EVICTION,
                        Optional.of(cause.toString())
                )));
    }

    private void registerLocalCacheMetrics(CaffeineCacheManager cacheManager) {
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = (Cache) cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache caffeineCache) {
                com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                registerCacheMetrics(cacheName, nativeCache);
            }
        });
    }

    private void registerCacheMetrics(String cacheName, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
        meterRegistry.gauge("cache.size", Tags.of("cache", cacheName), cache, this::getCacheSize);
        meterRegistry.gauge("cache.hits", Tags.of("cache", cacheName), cache, this::getCacheHits);
        meterRegistry.gauge("cache.misses", Tags.of("cache", cacheName), cache, this::getCacheMisses);
        meterRegistry.gauge("cache.evictions", Tags.of("cache", cacheName), cache, this::getCacheEvictions);
    }

    private double getCacheEvictions(com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
        return cache.stats().evictionCount();
    }

    private double getCacheMisses(com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
        return cache.stats().missCount();
    }

    private double getCacheHits(com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
        return cache.stats().hitCount();
    }

    private double getCacheSize(com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
        return cache.estimatedSize();
    }
}
