package com.micro.learningplatform.controllers;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
public class CacheTestController {

    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;


    @GetMapping("/print")
    public ResponseEntity<Map<String, Map<Object, Object>>> printCacheContents() {
        Map<String, Map<Object, Object>> cacheContents = new HashMap<>();
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache caffeineCache) {
                Map<Object, Object> cacheEntries = new HashMap<>(caffeineCache.getNativeCache().asMap());
                cacheContents.put(cacheName, cacheEntries);
            }
        });
        return ResponseEntity.ok(cacheContents);
    }


    @PostMapping("/put")
    public ResponseEntity<String> putInCache(@RequestParam String cacheName,
                                             @RequestParam String key,
                                             @RequestParam String value) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
            return ResponseEntity.ok("Value added to cache: " + cacheName + ", Key: " + key);
        }
        return ResponseEntity.badRequest().body("Cache not found: " + cacheName);
    }

    @GetMapping("/get")
    public ResponseEntity<Object> getFromCache(@RequestParam String cacheName,
                                               @RequestParam String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            Cache.ValueWrapper valueWrapper = cache.get(key);
            if (valueWrapper != null) {
                return ResponseEntity.ok(valueWrapper.get());
            }
            return ResponseEntity.status(404).body("Key not found: " + key);
        }
        return ResponseEntity.badRequest().body("Cache not found: " + cacheName);
    }


    @DeleteMapping("/clear")
    public ResponseEntity<String> clearCache(@RequestParam String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            return ResponseEntity.ok("Cache cleared: " + cacheName);
        }
        return ResponseEntity.badRequest().body("Cache not found: " + cacheName);
    }


    @GetMapping("/stats")
    public ResponseEntity<Map<String, CacheStatistics>> getCacheStatistics() {
        Map<String, CacheStatistics> stats = new HashMap<>();

        cacheManager.getCacheNames().forEach(cacheName -> {
            Gauge hitRateGauge = meterRegistry.find("cache.hit.rate")
                    .tag("cache", cacheName)
                    .gauge();
            Gauge sizeGauge = meterRegistry.find("cache.size")
                    .tag("cache", cacheName)
                    .gauge();

            stats.put(cacheName, new CacheStatistics(
                    cacheName,
                    hitRateGauge != null ? hitRateGauge.value() : 0.0,
                    sizeGauge != null ? sizeGauge.value() : 0.0
            ));
        });

        return ResponseEntity.ok(stats);
    }

    public  record CacheStatistics(
            String cacheName,
            double hitRate,
            double size
    ) {}
}

