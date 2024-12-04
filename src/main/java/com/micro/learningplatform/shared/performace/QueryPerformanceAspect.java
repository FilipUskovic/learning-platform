package com.micro.learningplatform.shared.performace;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/* Ista prica i za ovu metodu
@Aspect
@Component
@Slf4j
public class QueryPerformanceAspect {

    /*  Pratimo vrijeme izvrsavanja svakog upita i idencificiranje sporih
        Korištenje AOP-a (ascpect orientet programing) za praćenje vremena izvršavanja metoda označenih posebnom anotacijom
        globalno praćenje performansi bez promjena u kodu



    @Around("@annotation(QueryPerformance)")
    public Object measureQueryPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        long startTime = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long duration = (System.nanoTime() - startTime) / 1_000_000; // u milisekundama

            log.info("Method {} executed in {} ms", methodName, duration);

            // Spremamo statistiku za kasniju analizu
            QueryStatisticsCollector.recordQueryExecution(
                    methodName,
                    duration,
                    LocalDateTime.now()
            );
        }

    }
}
*/
