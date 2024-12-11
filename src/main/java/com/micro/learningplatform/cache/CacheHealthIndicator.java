package com.micro.learningplatform.cache;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class CacheHealthIndicator extends AbstractHealthIndicator {

    /* Ova je klasa koja kao sto i sama kaže radi healt cahks za hit/miss aka prikupljanje podatka iz keša
     *  1. imamo glavnu metodu koja radi logiku i dohvaca keš ovisno o tome za koji layer hvaat aka caffeine ili redis
     *  2. razdovio sam na 2 metode getCaffien i get RedisStats radi modularnosti i ako budem isto dodavao kasnije lakse je ubacit samo
     */

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    public CacheHealthIndicator(CacheManager cacheManager, MeterRegistry meterRegistry, RedisTemplate<String, Object> redisTemplate) {
        super("Cache health check");
        this.cacheManager = cacheManager;
        this.redisTemplate = redisTemplate;
    }


    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        Map<String, Object> details = new HashMap<>();
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache caffeineCache) {
                details.put(cacheName, getCaffeineStats(caffeineCache));
            } else if (cache instanceof RedisCache redisCache) {
                details.put(cacheName, getRedisStats(redisCache));
            } else {
                details.put(cacheName, "Cache type not supported");
            }
        }
        builder.up().withDetails(details);
    }

    private Map<String, Object> getCaffeineStats(CaffeineCache cache) {
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = cache.getNativeCache();
        CacheStats stats = nativeCache.stats();
        return Map.of(
                "size", nativeCache.estimatedSize(),
                "hitRate", stats.hitRate(),
                "missRate", stats.missRate(),
                "evictionCount", stats.evictionCount(),
                "averageLoadPenalty", stats.averageLoadPenalty()
        );
    }



    private Map<String, Object> getRedisStats(RedisCache cache) {
        String cacheName = cache.getName();

        // Dobivamo sve ključeve s prefiksom cacheName
        Set<String> keys = redisTemplate.keys(cacheName + "*");
        long size = keys != null ? keys.size() : 0;

        // Možete dodati dodatne statistike
        //TODO Ako je potrebno mogu dodati jos ttl i ostalu metriku
        Map<String, Object> stats = new HashMap<>();
        stats.put("size", size);
        stats.put("keys", keys);


        return stats;
    }

    /*
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
    }*/
}
