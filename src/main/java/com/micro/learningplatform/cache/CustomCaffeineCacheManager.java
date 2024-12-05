package com.micro.learningplatform.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class CustomCaffeineCacheManager extends CaffeineCacheManager {

    private final Map<String, Caffeine<Object, Object>> cacheBuilders = new HashMap<>();

    public void setCacheBuilders(Map<String, Caffeine<Object, Object>> builders) {
        cacheBuilders.putAll(builders);
    }


    @Override
    protected Cache createCaffeineCache(String name) {
        Caffeine<Object, Object> builder = cacheBuilders.getOrDefault(name,
                Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(Duration.ofMinutes(10)));
        return new CaffeineCache(name, builder.build());
    }
}
