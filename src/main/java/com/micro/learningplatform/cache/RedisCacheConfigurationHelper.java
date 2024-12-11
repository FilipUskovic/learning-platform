package com.micro.learningplatform.cache;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisCacheConfigurationHelper {

    /* Ova klas je zaduznea samo za redis configruaciju
     */

    private final RedisConnectionFactory redisConnectionFactory;
    private final MeterRegistry meterRegistry;

    @Value("${spring.cache.redis.time-to-live}")
    private Duration redisTtl;

    @Value("${spring.cache.redis.key-prefix}")
    private String keyPrefix;

    public RedisCacheManager createRedisCacheManager() {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(redisTtl)
                .prefixCacheNameWith(keyPrefix)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(createJsonSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigs = configureRedisCacheStrategies(defaultConfig);

        RedisCacheManager redisCacheManager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();

        // Registriramo metrike za Redis cache
        registerRedisMetrics(redisCacheManager);

        return redisCacheManager;
    }

    private Map<String, RedisCacheConfiguration> configureRedisCacheStrategies(RedisCacheConfiguration defaultConfig) {
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        for (CacheStrategy strategy : CacheStrategy.values()) {
            cacheConfigs.put(strategy.getSpec().name(),
                    defaultConfig.entryTtl(strategy.getSpec().expireAfterWrite()));
        }
        return cacheConfigs;
    }

    private RedisSerializer<Object> createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

  //  @EventListener(ApplicationReadyEvent.class) // za lazy evul
    private void registerRedisMetrics(RedisCacheManager redisCacheManager) {

        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            Properties info = connection.serverCommands().info(); // Dobivamo Redis informacije
            assert info != null;
            meterRegistry.gauge("redis.cache.keys", info, i -> Double.parseDouble(i.getProperty("db0.keys", "0")));
            meterRegistry.gauge("redis.cache.hits", info, i -> Double.parseDouble(i.getProperty("keyspace_hits", "0")));
            meterRegistry.gauge("redis.cache.misses", info, i -> Double.parseDouble(i.getProperty("keyspace_misses", "0")));
            meterRegistry.gauge("redis.cache.evictions", info, i -> Double.parseDouble(i.getProperty("evicted_keys", "0")));
        }
    }



}
