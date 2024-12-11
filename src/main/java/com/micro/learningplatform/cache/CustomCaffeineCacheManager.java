package com.micro.learningplatform.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CustomCaffeineCacheManager extends CaffeineCacheManager {

    private final Map<String, Caffeine<Object, Object>> cacheBuilders = new HashMap<>();
    private final CacheEventListener cacheEventListener;

    public CustomCaffeineCacheManager(CacheEventListener cacheEventListener) {
        this.cacheEventListener = cacheEventListener;
    }

    public void setCacheBuilders(Map<String, Caffeine<Object, Object>> builders) {
        cacheBuilders.putAll(builders);
    }

    // Dodao sam za pracenje događaja iz pomoc  removalListener i metode onCacheEvent
    // mogu lako ukljucvati prilagođene logike sada
    @Override
    protected Cache createCaffeineCache(String name) {
        Caffeine<Object, Object> builder = cacheBuilders.getOrDefault(name,
                Caffeine.newBuilder().
                        maximumSize(1000).expireAfterWrite(Duration.ofMinutes(10))
                        .recordStats()
                        .removalListener((key, value, cause) -> cacheEventListener.onCacheEvent(
                                new CacheEvent(name, key, CacheEvent.CacheEventType.EVICTION, Optional.of(cause.name()))
                        )));
        return new CaffeineCache(name, builder.build());
    }
}
