package com.micro.learningplatform.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
public class CacheTestController {

    private final CacheManager cacheManager;


    @GetMapping("/print")
    public ResponseEntity<String> printCacheContents() {
        StringBuilder cacheContents = new StringBuilder();
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache caffeineCache) {
                cacheContents.append("Cache: ").append(cacheName).append("\n");
                caffeineCache.getNativeCache().asMap().forEach((key, value) -> {
                    cacheContents.append("Key: ").append(key)
                            .append(", Value: ").append(value).append("\n");
                });
            }
        });
        return ResponseEntity.ok(cacheContents.toString());
    }

    @GetMapping("/put")
    public ResponseEntity<String> putInCache(@RequestParam String cacheName,
                                             @RequestParam String key,
                                             @RequestParam String value) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
            return ResponseEntity.ok("Value added to cache: " + cacheName);
        }
        return ResponseEntity.badRequest().body("Cache not found: " + cacheName);
    }

    @GetMapping("/get")
    public ResponseEntity<Object> getFromCache(@RequestParam String cacheName,
                                               @RequestParam String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            Cache.ValueWrapper valueWrapper = cache.get(key);
            return ResponseEntity.ok(valueWrapper != null ? valueWrapper.get() : "Key not found");
        }
        return ResponseEntity.badRequest().body("Cache not found: " + cacheName);
    }

    @GetMapping("/clear")
    public ResponseEntity<String> clearCache(@RequestParam String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            return ResponseEntity.ok("Cache cleared: " + cacheName);
        }
        return ResponseEntity.badRequest().body("Cache not found: " + cacheName);
    }
}
