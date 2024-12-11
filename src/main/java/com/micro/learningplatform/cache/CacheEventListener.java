package com.micro.learningplatform.cache;

import com.micro.learningplatform.services.CacheMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CacheEventListener {

    private final CacheMetricsService metricsService;

    /*
     * imao sam duplicirane koda i logike ovdje kod bilježenja događaja i metrike
     * radi bolje modularnosti ov klasa ce upravljati samo samo jednom odgovornosti
     *
     *          "TE SADA JE SAMO ULAZNA TOČKA DOGAĐAJA "
     *
     *  Sto postižem s tim :
     *  1. sada se logika događa samo na jednom mjestu -> CacheMetricsService
     *  2. ova klasa sada ima samo jednu odgovornost
     *  3. lakse je testirati
     */

    public void onCacheEvent(CacheEvent cacheEvent) {
        // delegiramo dogadaj u cacheMetrics za centralizirano upravljanje
        metricsService.recordCacheEvent(cacheEvent);

        log.debug("Processed cache event: type={}, cache={}, key={}",
                cacheEvent.type(), cacheEvent.cacheName(), cacheEvent.key());
    }
}
