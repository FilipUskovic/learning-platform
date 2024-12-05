package com.micro.learningplatform.shared;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimiterManager {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Implemetacije rate limitera korsiteci bucket4j algoritam
     * omogucava nam preciznu kontrolu nad brojem ahtijeva, koje pojedini klijenot moze napraviti u odredeno vrijeme
     */

    public Bucket resolveBucket(String clientId) {
        return buckets.computeIfAbsent(clientId, this::createNewBucket);
    }

    public Bucket createNewBucket (String clientId){
        // početno vrijeme poravnanja na početak sljedeće minute
        Instant align = Instant.now()
                .plus(1, ChronoUnit.MINUTES)
                .truncatedTo(ChronoUnit.MINUTES);

        // strategija punjenja s poravnanjem

        Bandwidth limit = Bandwidth.builder()
                .capacity(100)
                .refillIntervallyAligned(100, Duration.ofMinutes(1), align)
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
