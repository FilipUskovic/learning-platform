package com.micro.learningplatform.cache;

import com.micro.learningplatform.services.CacheMetricsService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.Cache;

import java.util.Optional;
import java.util.concurrent.Callable;

/* Klasa koja se baci pracenjem kesiranja
 * 1. dodao sam klasu monitorCacheOperation za dupiciranje get metoda sada korsite zajednickiu pomocnu metodu
 *    te pojednostavnio implementaciju s tim pomocnom metodom
 *  2. centralizirao hit i miss ogadaje
 */

public class MonitoredCache implements Cache {
    private static final Logger log = LogManager.getLogger(MonitoredCache.class);
    private final Cache delegate;
    private final String name;
    private final CacheMetricsService metricsService;

    public MonitoredCache(
            Cache delegate,
            String name,
            CacheMetricsService metricsService) {
        this.delegate = delegate;
        this.name = name;
        this.metricsService = metricsService;
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return delegate.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        return monitorCacheOperation(() -> delegate.get(key), key, CacheEvent.CacheEventType.HIT, CacheEvent.CacheEventType.MISS);

    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        return monitorCacheOperation(() -> delegate.get(key, type), key, CacheEvent.CacheEventType.HIT, CacheEvent.CacheEventType.MISS);

    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        return monitorCacheOperation(() -> delegate.get(key, valueLoader), key, CacheEvent.CacheEventType.HIT, CacheEvent.CacheEventType.MISS);

    }

    @Override
    public void put(Object key, Object value) {
        delegate.put(key, value);
        metricsService.recordCacheEvent(new CacheEvent(name, key, CacheEvent.CacheEventType.PUT, Optional.empty()));

    }

    @Override
    public void evict(Object key) {
        delegate.evict(key);
        metricsService.recordCacheEvent(new CacheEvent(name, key, CacheEvent.CacheEventType.EVICTION, Optional.empty()));


    }

    @Override
    public void clear() {
        delegate.clear();
        log.info("Cache '{}' cleared.", name);
    }

    // Centraliziran metoda za biljezenje hit i miss-a smanjuje dupliciranje get metoda
    private <T> T monitorCacheOperation(Callable<T> operation, Object key, CacheEvent.CacheEventType hitType, CacheEvent.CacheEventType missType) {
        try {
            T result = operation.call();
            CacheEvent.CacheEventType eventType = (result != null) ? hitType : missType;
            metricsService.recordCacheEvent(new CacheEvent(name, key, eventType, Optional.empty()));
            return result;
        } catch (Exception e) {
            log.error("Cache operation failed for cache={}, key={}", name, key, e);
            throw new RuntimeException(e);
        }
    }


}
