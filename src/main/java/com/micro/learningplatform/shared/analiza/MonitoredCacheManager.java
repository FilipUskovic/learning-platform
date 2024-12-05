package com.micro.learningplatform.shared.analiza;

import com.micro.learningplatform.cache.MonitoredCache;
import com.micro.learningplatform.services.CacheMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

import java.util.Collection;

// Wrapper za CacheManager koji dodaje monitoring

@RequiredArgsConstructor
public class MonitoredCacheManager implements CacheManager {
    private final CacheManager delegate;
    private final CacheMetricsService metricsService;

    @Override
    public Cache getCache(String name) {
        Cache cache = delegate.getCache(name);
        return cache != null ? new MonitoredCache(cache, name, metricsService) : null;
    }

    @Override
    @Nullable
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }
}
