package com.micro.learningplatform.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.micro.learningplatform.config.CacheSpec;
import com.micro.learningplatform.config.CustomCaffeineCacheManager;
import com.micro.learningplatform.services.CacheMetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.github.benmanes.caffeine.cache.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@RequiredArgsConstructor
@Slf4j
public class CentralizdCacheConfiguration implements CachingConfigurer {

    private final RedisConnectionFactory redisConnectionFactory;
    private final MeterRegistry meterRegistry;
    private final CacheMetricsService cacheMetricsService;

    @Value("${spring.cache.caffeine.spec}")
    private String caffeineSpec;

    @Value("${spring.cache.redis.time-to-live}")
    private Duration redisTtl;

    @Value("${spring.cache.redis.key-prefix}")
    private String keyPrefix;


    private enum CacheStrategy {
        REFERENCE(CacheSpec.builder()
                .name("reference")
                .maxSize(1000)
                .expireAfterWrite(Duration.ofHours(1))
                .build()),

        FREQUENT(CacheSpec.builder()
                .name("frequent")
                .maxSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build()),

        SEARCH(CacheSpec.builder()
                .name("search")
                .maxSize(1000)
                .expireAfterWrite(Duration.ofMinutes(1))
                .build());

        private final CacheSpec spec;

        CacheStrategy(CacheSpec spec) {
            this.spec = spec;
        }

    }


    // kompozitni manager koji pravlja lokalnim i distributivnim kesom
    @Override
    @Bean
    public CacheManager cacheManager() {
        CompositeCacheManager compositeCacheManager = new CompositeCacheManager();

       // lokalni cache (Caffeine) za brzi pristup
        CaffeineCacheManager localCache = createLocalCacheManager();
       // distribuirani cache (Redis) za dijeljene podatke
        RedisCacheManager distributedCache = createDistributedCacheManager();

        compositeCacheManager.setCacheManagers(Arrays.asList(
                localCache,
                distributedCache
        ));

        // Omogućavamo fallback ako primarni cache nije dostupan
        compositeCacheManager.setFallbackToNoOpCache(true);
        return compositeCacheManager;

    }



    //  distribuirani Redis cache s proper serijalizacijom i TTL strategijom
    private RedisCacheManager createDistributedCacheManager() {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(redisTtl)
                .prefixCacheNameWith(keyPrefix)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(createJsonSerializer()))
                .computePrefixWith(cacheName -> "cache:" + cacheName + ":");

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // Konfiguriramo Redis cache za svaku strategiju
        for (CacheStrategy strategy : CacheStrategy.values()) {
            CacheSpec spec = strategy.spec;
            log.info("Configuring Redis cache for: {}", spec.name());
            cacheConfigs.put(spec.name(),
                    defaultConfig.entryTtl(spec.expireAfterWrite()));
        }

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }


    // okalni cache s različitim strategijama za različite tipove podataka
    private CustomCaffeineCacheManager createLocalCacheManager() {
        CustomCaffeineCacheManager cacheManager = new CustomCaffeineCacheManager();
        Map<String, Caffeine<Object, Object>> cacheBuilders = new HashMap<>();

        // Konfiguriramo cache za svaku strategiju
        for (CacheStrategy strategy : CacheStrategy.values()) {
            CacheSpec spec = strategy.spec;
            cacheBuilders.put(spec.name(),
                    Caffeine.newBuilder()
                            .maximumSize(spec.maxSize())
                            .expireAfterWrite(spec.expireAfterWrite())
                            .recordStats()
                            .removalListener((key, value, cause) ->
                                    handleCacheRemoval(spec.name(), key, cause))
            );
            log.info("Configured cache: {}, maxSize: {}, expireAfterWrite: {}",
                    spec.name(), spec.maxSize(), spec.expireAfterWrite());
        }
        cacheManager.setCacheBuilders(cacheBuilders);
        registerLocalCacheMetrics(cacheManager);

        return cacheManager;
    }

    // Handler za praćenje izbacivanja podataka iz cache-a
    private void handleCacheRemoval(String cacheName, Object key, RemovalCause cause) {
        cacheMetricsService.recordCacheEviction(cacheName, key.toString(), cause.toString());

        if (cause.equals(RemovalCause.SIZE)) {
            log.warn("Cache {} evicted entry {} due to size constraints",
                    cacheName, key);
        }
    }

    // metrike za praćenje performansi lokalnog cache-a
    private void registerLocalCacheMetrics(CaffeineCacheManager cacheManager) {
       cacheManager.getCacheNames().forEach(cacheName -> {
           Cache cache = cacheManager.getCache(cacheName);
           if (cache instanceof CaffeineCache) {
               com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                       ((CaffeineCache) cache).getNativeCache();

               // Registriramo različite metrike
               registerCacheMetrics(cacheName, nativeCache);
           }
       });
    }

    private void registerCacheMetrics(String cacheName, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
        // Osnovne metrike
        meterRegistry.gauge("cache.size",
                Tags.of("cache", cacheName),
                cache,
                this::getCacheSize);

        // Hit/miss ratio
        meterRegistry.gauge("cache.hits",
                Tags.of("cache", cacheName),
                cache,
                this::getCacheHits);

        meterRegistry.gauge("cache.misses",
                Tags.of("cache", cacheName),
                cache,
                this::getCacheMisses);

        // Eviction metrike
        meterRegistry.gauge("cache.evictions",
                Tags.of("cache", cacheName),
                cache,
                this::getCacheEvictions);
    }

    private double getCacheEvictions(com.github.benmanes.caffeine.cache.Cache<Object, Object> objectObjectCache) {
        return objectObjectCache.stats().evictionCount();
    }

    private double getCacheMisses(com.github.benmanes.caffeine.cache.Cache<Object, Object> objectObjectCache) {
        return objectObjectCache.stats().missCount();
    }

    private double getCacheHits(com.github.benmanes.caffeine.cache.Cache<Object, Object> objectObjectCache) {
       return objectObjectCache.stats().hitCount();
    }

    private double getCacheSize(com.github.benmanes.caffeine.cache.Cache<Object, Object> objectObjectCache) {
        return objectObjectCache.estimatedSize();
        
    }


    private RedisSerializer<Object> createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new GenericJackson2JsonRedisSerializer(objectMapper);

    }





}
