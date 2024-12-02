package com.micro.learningplatform.config;

import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.jsr107.Eh107Configuration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

@Configuration
@EnableCaching
public class EhCacheConfig {

    //TODO: promjeniti javax u jakartu ne zelim korisiti zastarijele verzije i nacine
    //TODO: Razmisliti o integriranom springboot kesiranju da bi pojednostavio konfiguraciju
    // TODO: Eh107Configuration je cesto nepotreban kod moderne hibernate jcache-a

    /**
     * Stvara CacheManager koja upravlja kesiranjem
     * Smanjuje ucestalo poziva prema bazi, time povecava brzinu applikacije
     * ->
     */

    @Bean(name = "customCacheManager")
    public CacheManager cacheManager() {
        // Kreiranje Ehcache CacheManager-a
        org.ehcache.CacheManager ehCacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("defaultCache",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                Object.class, Object.class, ResourcePoolsBuilder.heap(100))
                )
                .build(true);

        // Dobivanje CachingProvider instance
        CachingProvider cachingProvider = Caching.getCachingProvider();
        // Dobivanje zadane CacheManager instance
        CacheManager cacheManager = cachingProvider.getCacheManager();
        // Kreiranje keša s Ehcache konfiguracijom
        cacheManager.createCache("defaultCache",
                Eh107Configuration.fromEhcacheCacheConfiguration(
                        ehCacheManager.getRuntimeConfiguration()
                                .getCacheConfigurations().get("defaultCache")
                )
        );
        return cacheManager;
    }

}