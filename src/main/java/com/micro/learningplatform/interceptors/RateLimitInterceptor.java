package com.micro.learningplatform.interceptors;

import com.micro.learningplatform.shared.RateLimiterManager;
import com.micro.learningplatform.shared.exceptions.RateLimitExceededException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterManager rateLimiterManager;



    @SneakyThrows
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String cliendId = extractClientId(request);
        Bucket bucket = rateLimiterManager.resolveBucket(cliendId);

        // Pokušavamo potrošiti 1 token i dobivamo informacije o potrošnji
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        // Provjeravamo imamo li još dostupnih tokena
        if (!probe.isConsumed()) {
           // Dodajemo korisne informacije u response
            response.addHeader("X-RateLimit-Limit", "100");
            response.addHeader("X-RateLimit-Remaining", "0");
            response.addHeader("X-RateLimit-Reset",
                    String.valueOf(calculateResetTime(probe)));

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            throw new RateLimitExceededException(
                    "Rate limit exceeded for client: " + cliendId);

        }

        return true;
    }

    private String extractClientId(HttpServletRequest request) {
        // Prvo pokušavamo iz API ključa
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null) {
            return apiKey;
        }
        // Ako nemamo API ključ, koristimo IP adresu
        return request.getRemoteAddr();
    }

    private long calculateResetTime(ConsumptionProbe probe) {
        // Vraća vrijeme u sekundama do sljedećeg punjenja tokena
        return probe.getNanosToWaitForRefill() / 1_000_000_000;
    }
}
