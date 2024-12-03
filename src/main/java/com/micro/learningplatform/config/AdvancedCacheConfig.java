package com.micro.learningplatform.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Arrays;

@Configuration
public class AdvancedCacheConfig {

    //TODO osigurati sinkronizaciju izmedu caffeine i redis-a i metrika za pracenje slojeva

    /**
     * Koristi vise slojni kesiranje koristeci CompositeCacheManager
     * - CaffeineCacheManager za lokalno kesiranje (brzo u memoriji)
     * - RedisCacheManager za distributivni keš (širi doseg i persistencija)
     * kombiniram prednosti viseslojnig kesirajna (lokalnog i distibutivnoga)
     */


    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        CompositeCacheManager compositeCacheManager = new CompositeCacheManager();

        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .recordStats());

        RedisCacheManager redisCacheManager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofHours(1))
                        .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())))
                .build();

        // Postavljamo listu CacheManagera
        compositeCacheManager.setCacheManagers(Arrays.asList(caffeineCacheManager, redisCacheManager));

        return compositeCacheManager;
    }


}
