package com.micro.learningplatform.config;

import com.micro.learningplatform.cache.*;
import com.micro.learningplatform.services.CacheMetricsService;
import com.micro.learningplatform.shared.analiza.MonitoredCacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableCaching
@RequiredArgsConstructor
@Slf4j
public class CentralizdCacheConfiguration implements CachingConfigurer {

    /* Ova se sada služi kao orkestrator cija je jedina odgovornost povezati redis  caffeince confgiguraciju
     * lakse je sada nadodati i vie je citljiva klasa nije zaduzena za logiku i metrice u isto vrijeme
      -> jednostavnijia je jer su dgovornosti razdovjene na pomocne klase
     */

    private final RedisCacheConfigurationHelper redisHelper;
    private final CaffeineCacheConfigurationHelper caffeineHelper;
    private final CacheMetricsService cacheMetricsService;


    // kompozitni manager koji pravlja lokalnim i distributivnim kesom
    @Override
    @Bean
    public CacheManager cacheManager() {
        // Kreiramo CompositeCacheManager koji podržava i Redis i Caffeine
        CompositeCacheManager compositeCacheManager = new CompositeCacheManager(
                createMonitoredCacheManager(caffeineHelper.createCaffeineCacheManager()),
                createMonitoredCacheManager(redisHelper.createRedisCacheManager())
        );

        // Fallback opcija ako cache nije dostupan
        compositeCacheManager.setFallbackToNoOpCache(true);
        return compositeCacheManager;

    }

    private CacheManager createMonitoredCacheManager(CacheManager cacheManager) {
        return new MonitoredCacheManager(cacheManager, cacheMetricsService);
    }










}
