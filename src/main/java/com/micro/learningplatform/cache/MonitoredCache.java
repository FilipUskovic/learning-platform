package com.micro.learningplatform.cache;

import com.micro.learningplatform.services.CacheMetricsService;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

// Wrapper za Cache koji dodaje monitoring

public class MonitoredCache implements Cache {
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
        ValueWrapper value = delegate.get(key);
        if (value != null) {
            metricsService.recordCacheHit(name);
        } else {
            metricsService.recordCacheMiss(name);
        }
        return value;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        metricsService.recordCacheHit(name);
        return delegate.get(key, type);
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        metricsService.recordCacheHit(name);
        return delegate.get(key, valueLoader);
    }

    @Override
    public void put(Object key, Object value) {
      //  metricsService.recordCachePut(name);
        delegate.put(key, value);
    }

    @Override
    public void evict(Object key) {
    //   metricsService.recordCacheEvict(name);
        delegate.evict(key);
    }

    @Override
    public void clear() {
    //    metricsService.recordCacheClear(name);
        delegate.clear();

    }


}
