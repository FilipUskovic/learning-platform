package com.micro.learningplatform.services;

import com.micro.learningplatform.cache.CacheEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheMetricsService {


    /* ovo je servis koji prati upravljanjem metrike i statistikama
     * Ovjde sada isto cemo razdvojit ovu klasu da samo upravlja metrikama tj logikom
     * 1. nije uvjek nuzno potrebno razdvajati sve klase ali posto vec imam slozenijji kesiranje ima smisla
     * 2 . zelim bolju modulranost
     * 3. znam da ce jos rast slozenost i kod pa ima smisla razdoviti
     * 4.
     */

    private final MeterRegistry meterRegistry;
    private final Map<String, AtomicLong> cacheHits = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> cacheMisses = new ConcurrentHashMap<>();

    public void recordMetrics(String metricName, String cacheName, Map<String, String> tags) {
        Tags micrometerTags = Tags.of(
                tags.entrySet().stream()
                        .flatMap(entry -> Stream.of(entry.getKey(), entry.getValue()))
                        .toArray(String[]::new)
        );

        log.debug("Recording metric: name={}, cache={}, tags={}", metricName, cacheName, tags);
        meterRegistry.counter(metricName, micrometerTags).increment();
    }


    public void recordCacheEvent(CacheEvent event) {
        switch (event.type()) {
            case HIT -> recordCacheHit(event.cacheName());
            case MISS -> recordCacheMiss(event.cacheName());
            case PUT -> recordCachePut(event.cacheName(), event.key());
            case EVICTION -> recordCacheEviction(event.cacheName(), event.key(), event.evictionReason().orElse("unknown"));
        }

        // Logiramo svaki događaj za analizu
        log.debug("Cache event recorded: type={}, cache={}, key={}, reason={}",
                event.type(), event.cacheName(), event.key(), event.evictionReason().orElse("N/A"));
    }


    public void recordCacheHit(String cacheName) {
        cacheHits.computeIfAbsent(cacheName, key -> new AtomicLong(0)).incrementAndGet();
        updateHitRateMetric(cacheName);
        meterRegistry.counter("cache.hit", "cache", cacheName).increment();
    }

    public void recordCacheMiss(String cacheName) {
        cacheMisses.computeIfAbsent(cacheName, key -> new AtomicLong(0)).incrementAndGet();
        updateHitRateMetric(cacheName);
        meterRegistry.counter("cache.miss", "cache", cacheName).increment();
    }

    public void recordCachePut(String cacheName, Object key) {
        meterRegistry.counter("cache.put", "cache", cacheName).increment();
        log.debug("Cache put: cache={}, key={}", cacheName, key);
    }

    public void recordCacheEviction(String cacheName, Object key, String reason) {
        meterRegistry.counter("cache.eviction", "cache", cacheName, "reason", reason).increment();
        log.warn("Cache eviction: cache={}, key={}, reason={}", cacheName, key, reason);
    }



    private void updateHitRateMetric(String cacheName) {
        long hits = cacheHits.getOrDefault(cacheName, new AtomicLong(0)).longValue();
        long misses = cacheMisses.getOrDefault(cacheName, new AtomicLong(0)).longValue();
        double hitRate = (hits + misses == 0) ? 0.0 : (double) hits / (hits + misses);

        meterRegistry.gauge("cache.hit.rate", Tags.of("cache", cacheName), hitRate);
        log.debug("Cache '{}' hit rate updated: {}", cacheName, hitRate);
    }

}
